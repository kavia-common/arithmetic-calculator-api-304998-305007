from fabric import task, Connection
from invoke import UnexpectedExit, run as local
import os
import datetime
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# ========== CONFIGURATION ==========
REMOTE_HOST = "54.189.2.248"
REMOTE_USER = "ubuntu"
HOST_STRING = f"{REMOTE_USER}@{REMOTE_HOST}"

REMOTE_WORK_DIR = "/home/ubuntu/spring_app_build"
# Store log in HOME to avoid path conflicts during cleanup
REMOTE_LOG_FILE = "build_output.log" 

# --- Local Log Configuration ---
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
LOCAL_LOG_PATH = os.path.join(CURRENT_DIR, "logs")
os.makedirs(LOCAL_LOG_PATH, exist_ok=True)
# ===================================

@task
def build(c):
    """
    If the repo exists: performs git pull.
    If it does not exist: performs git clone.
    Then dynamically enters the project folder and runs the build.
    """
    print("🔎 Validating configuration...")
    
    ssh_pass = os.environ.get("SSH_PASSWORD", "").strip()
    if not ssh_pass:
        print("❌ ERROR: SSH_PASSWORD not found in .env file")
        return

    # 1. Detect local Git info
    try:
        current_branch = local('git rev-parse --abbrev-ref HEAD', hide=True).stdout.strip()
        repo_url = local('git remote get-url origin', hide=True).stdout.strip()
        print(f"   Detected branch: {current_branch}")
        print(f"   Repo URL: {repo_url}")
    except UnexpectedExit:
        print("❌ Error: Run this from a Git repository.")
        return

    # 2. Connection
    print(f"🔌 Connecting to {HOST_STRING}...")
    conn = Connection(HOST_STRING, connect_kwargs={"password": ssh_pass})

    timestamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    local_log_file = os.path.join(LOCAL_LOG_PATH, f"build_{current_branch}_{timestamp}.log")

    try:
        print(f"🚀 Starting operation in: {REMOTE_WORK_DIR}")
        
        # --- BASH SCRIPT LOGIC ---
        remote_script = f"""
        # 1. Decide whether to Clone or Update
        if [ -d "{REMOTE_WORK_DIR}/.git" ]; then
            echo "🔄 Repository exists. Updating branch '{current_branch}'..."
            cd {REMOTE_WORK_DIR}
            
            # Ensure we have the latest remote changes
            git fetch origin
            
            # Force checkout to the desired branch (creating it if it doesn't exist)
            git checkout {current_branch} || git checkout -b {current_branch} origin/{current_branch}
            
            # Pull changes
            git pull origin {current_branch}
        else
            echo "🆕 Repository does not exist. Cloning branch '{current_branch}'..."
            mkdir -p {REMOTE_WORK_DIR}
            cd {REMOTE_WORK_DIR}
            git clone -b {current_branch} {repo_url} .
        fi

        # 2. Enter dynamic sub-folder (where gradlew resides)
        # Ensure we are at the repo root first
        cd {REMOTE_WORK_DIR}
        
        # Find the first visible directory
        PROJECT_DIR=$(ls -d */ | head -n 1)
        
        if [ -z "$PROJECT_DIR" ]; then
            echo "❌ Error: Project folder not found inside repo."
            exit 1
        fi

        cd "$PROJECT_DIR"
        echo "📂 Entering project directory: $(pwd)"

        # 3. Execute Build
        chmod +x gradlew
        ./gradlew build
        """

        # Wrap everything to capture logs with 'tee'
        full_command = f"({remote_script}) 2>&1 | tee ~/{REMOTE_LOG_FILE}"

        print("⏳ Executing synchronization and build...")
        conn.run(full_command, pty=True)

        print("\n⬇️ Downloading logs...")
        conn.get(f"/home/{REMOTE_USER}/{REMOTE_LOG_FILE}", local_log_file)
        print(f"✅ Log saved to: {local_log_file}")

    except UnexpectedExit:
        print(f"\n❌ Build failed. Retrieving logs...")
        try:
            conn.get(f"/home/{REMOTE_USER}/{REMOTE_LOG_FILE}", local_log_file)
            print(f"⚠️ Error log saved to: {local_log_file}")
        except:
            print("Could not retrieve remote log.")
            
    finally:
        conn.close()
        print("🔌 Connection closed.")