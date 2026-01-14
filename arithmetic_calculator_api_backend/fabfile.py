from fabric import task, Connection
from invoke import UnexpectedExit, run as local
import os
import datetime
from dotenv import load_dotenv

# Cargar variables de entorno
load_dotenv()

# ========== CONFIGURACIÓN ==========
REMOTE_HOST = "54.189.2.248"
REMOTE_USER = "ubuntu"
HOST_STRING = f"{REMOTE_USER}@{REMOTE_HOST}"

REMOTE_WORK_DIR = "/home/ubuntu/spring_app_build"
# Guardamos log en el HOME para evitar conflictos de rutas
REMOTE_LOG_FILE = "build_output.log" 

# --- Configuración de Logs Locales ---
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
LOCAL_LOG_PATH = os.path.join(CURRENT_DIR, "logs")
os.makedirs(LOCAL_LOG_PATH, exist_ok=True)
# ===================================

@task
def build(c):
    """
    Si el repo existe: hace git pull.
    Si no existe: hace git clone.
    Luego entra dinámicamente a la carpeta del proyecto y compila.
    """
    print("🔎 Validando configuración...")
    
    ssh_pass = os.environ.get("SSH_PASSWORD", "").strip()
    if not ssh_pass:
        print("❌ ERROR: SSH_PASSWORD no encontrada en .env")
        return

    # 1. Detectar info local
    try:
        current_branch = local('git rev-parse --abbrev-ref HEAD', hide=True).stdout.strip()
        repo_url = local('git remote get-url origin', hide=True).stdout.strip()
        print(f"   Rama detectada: {current_branch}")
        print(f"   Repo URL: {repo_url}")
    except UnexpectedExit:
        print("❌ Error: Ejecuta esto desde un repo Git.")
        return

    # 2. Conexión
    print(f"🔌 Conectando a {HOST_STRING}...")
    conn = Connection(HOST_STRING, connect_kwargs={"password": ssh_pass})

    timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    local_log_file = os.path.join(LOCAL_LOG_PATH, f"build_{current_branch}_{timestamp}.log")

    try:
        print(f"🚀 Iniciando operación en: {REMOTE_WORK_DIR}")
        
        # --- LÓGICA INTELIGENTE EN BASH ---
        # Usamos comillas triples f-string para escribir el script de bash legiblemente
        remote_script = f"""
        # 1. Decidir si Clonar o Actualizar
        if [ -d "{REMOTE_WORK_DIR}/.git" ]; then
            echo "🔄 El repositorio ya existe. Actualizando rama '{current_branch}'..."
            cd {REMOTE_WORK_DIR}
            
            # Aseguramos que tenemos los últimos cambios del remoto
            git fetch origin
            
            # Forzamos el checkout a la rama deseada (creándola si no existe localmente)
            git checkout {current_branch} || git checkout -b {current_branch} origin/{current_branch}
            
            # Traemos los cambios
            git pull origin {current_branch}
        else
            echo "🆕 El repositorio no existe. Clonando rama '{current_branch}'..."
            mkdir -p {REMOTE_WORK_DIR}
            cd {REMOTE_WORK_DIR}
            git clone -b {current_branch} {repo_url} .
        fi

        # 2. Entrar a la sub-carpeta dinámica (donde está el gradlew)
        # Nos aseguramos de estar en la raíz del repo primero
        cd {REMOTE_WORK_DIR}
        
        # Buscamos el primer directorio visible
        PROJECT_DIR=$(ls -d */ | head -n 1)
        
        if [ -z "$PROJECT_DIR" ]; then
            echo "❌ Error: No se encontró la carpeta del proyecto dentro del repo."
            exit 1
        fi

        cd "$PROJECT_DIR"
        echo "📂 Entrando a directorio del proyecto: $(pwd)"

        # 3. Ejecutar Build
        chmod +x gradlew
        ./gradlew build
        """

        # Colapsamos los saltos de línea para enviarlo como una sola instrucción larga,
        # pero mantenemos los puntos y coma necesarios.
        # (Fabric maneja bien los scripts multilinea si no son muy complejos, 
        # pero es más seguro envolverlo todo para el 'tee')
        
        full_command = f"({remote_script}) 2>&1 | tee ~/{REMOTE_LOG_FILE}"

        print("⏳ Ejecutando sincronización y build...")
        conn.run(full_command, pty=True)

        print("\n⬇️ Descargando logs...")
        conn.get(f"/home/{REMOTE_USER}/{REMOTE_LOG_FILE}", local_log_file)
        print(f"✅ Log guardado en: {local_log_file}")

    except UnexpectedExit:
        print(f"\n❌ El build falló. Recuperando logs...")
        try:
            conn.get(f"/home/{REMOTE_USER}/{REMOTE_LOG_FILE}", local_log_file)
            print(f"⚠️ Log de error guardado en: {local_log_file}")
        except:
            print("No se pudo recuperar el log remoto.")
            
    finally:
        conn.close()
        print("🔌 Conexión cerrada.")