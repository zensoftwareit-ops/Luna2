# Portainer Configuration Guide
# 
# Portainer is a lightweight management UI for Docker.
# It provides a web interface to manage containers, images, volumes, and networks.

## Quick Start

### Enable Portainer

1. Open `docker-compose-preprod.yml`

2. Find the commented `portainer:` section (around line 140):

```yaml
# portainer:
#   image: portainer/portainer-ce:latest
```

3. Uncomment all lines (remove `# ` prefix):

```yaml
portainer:
  image: portainer/portainer-ce:latest
  container_name: luna2-portainer
  
  ports:
    - "9000:9000"
    - "8000:8000"
  
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
    - portainer_data:/data
  
  networks:
    - luna2-network
  
  restart: unless-stopped
```

4. Start Portainer:

```bash
docker-compose -f docker-compose-preprod.yml up -d portainer
```

5. Wait 15 seconds for it to start:

```bash
docker-compose -f docker-compose-preprod.yml logs portainer
```

6. Access: http://your-server-ip:9000


### Initial Setup

**First Time:**

1. Create admin account
   - Username: (choose)
   - Password: (strong password - min 8 chars)

2. Select Environment: "Local"

3. Click "Connect"

**⚠️ Security:**
- Change default password immediately
- Use strong passwords
- Don't expose port 9000 publicly (no external access)
- Consider adding firewall rule: only your IP can access 9000


## Main Features

### 1. Dashboard

Overview of all containers, images, volumes, networks.

**What you see:**
- Total containers (running/stopped)
- Images (count)
- Volumes (count)
- Networks (count)
- System resource usage

### 2. Containers

Manage individual Docker containers.

**Available actions:**
- ✓ Start/Stop containers
- ✓ Restart containers
- ✓ Remove containers
- ✓ View logs
- ✓ Execute commands in container
- ✓ Inspect (view config, env vars, mounts)
- ✓ Stats (CPU, memory, network usage)

**Example: Restart Luna2 API**
1. Click Containers
2. Find "luna2-api"
3. Click on it
4. Scroll down and click "Restart"

**Example: View Luna2 API Logs**
1. Click Containers
2. Find "luna2-api"
3. Click on it
4. Scroll to "Logs" tab
5. View output (tail of logs)

### 3. Images

Browse and manage Docker images.

**Actions:**
- ✓ Pull new images from Docker Hub
- ✓ Remove unused images
- ✓ View image details (layers, size, repo)
- ✓ Build from Dockerfile (advanced)

### 4. Volumes

Manage Docker persistentvolumes.

**Luna2 volumes:**
- `mysql_data` - MySQL database files
- `portainer_data` - Portainer configuration

**Actions:**
- ✓ View volume details
- ✓ Remove unused volumes
- ✓ Browse files in volume (advanced)

### 5. Networks

View Docker networks and connected containers.

**Luna2 network:**
- `luna2-network` - Bridge network connecting all containers
  - mysql (172.28.0.2)
  - luna2-api (172.28.0.3)
  - nginx (172.28.0.4)
  - portainer (172.28.0.5)

### 6. Settings

Configure Portainer behavior.

**Available:**
- Docker endpoint settings
- Authentication settings
- Environment settings


## Common Tasks

### Monitor Resource Usage

1. Home → Stats tab at bottom
2. Or: Containers → Click container → Stats tab

**What each metric means:**
- CPU %: CPU cores being used
- Memory: RAM being used
- Network I/O: Data in/out per second

### View Database Logs

1. Containers → luna2-mysql
2. Logs tab
3. Watch for errors containing "ERROR"

### Restart All Containers

1. Home
2. Click each container: Start/Restart button

Or from terminal:

```bash
docker-compose -f docker-compose-preprod.yml restart
```

### Clean Up Unused Images

1. Images
2. Select unused images
3. Click "Remove"

Or from terminal:

```bash
docker system prune -a
# Removes all unused images, containers, volumes
```

### Backup Portainer Data

```bash
# Backup Portainer settings (if needed for migration)
docker run --rm -v portainer_data:/portainer -v $(pwd):/backup \
  alpine tar czf /backup/portainer-backup.tar.gz -C / portainer
```


## Security Best Practices

✓ **DO:**
- Change default password on first login
- Use strong passwords (16+ chars recommended)
- Restrict access to port 9000 via firewall
- Regularly check "Logs" for errors
- Use Portainer read-only mode for non-admins (in team setup)

✗ **DON'T:**
- Expose port 9000 publicly (remove from docker-compose)
- Reuse passwords from other systems
- Give access to everyone on the network
- Run container commands you don't understand
- Stop database container without backup


## Troubleshooting

### Port 9000 already in use

```bash
# Find what's using port 9000
lsof -i :9000

# If it's another service, stop it:
sudo systemctl stop other-service

# Or start Portainer on different port:
# Edit docker-compose.yml:
# - "9001:9000"  # Change 9000 to 9001
```

### Portainer won't start

```bash
# Check logs
docker-compose -f docker-compose-preprod.yml logs portainer

# Ensure docker.sock is accessible
ls -la /var/run/docker.sock
# Should show: srw-rw---- root docker

# If permission issue:
sudo chmod g+w /var/run/docker.sock
```

### Can't access http://ip:9000

1. Check if running:
   ```bash
   docker-compose -f docker-compose-preprod.yml ps portainer
   # Should show: luna2-portainer  Up
   ```

2. Check firewall:
   ```bash
   sudo ufw allow 9000
   ```

3. Check from server:
   ```bash
   curl http://localhost:9000
   # Should get HTML response
   ```

4. Check from local computer:
   ```bash
   curl http://your-server-ip:9000
   # Should get HTML response
   ```

### Forgotten Portainer password

```bash
# Only option: remove and recreate
docker-compose -f docker-compose-preprod.yml down portainer

# Remove data
docker volume rm portainer_data

# Restart portainer - will ask for new credentials
docker-compose -f docker-compose-preprod.yml up -d portainer
```


## Alternative: Command Line Only

If you prefer NOT to use Portainer, all management can be done via CLI:

```bash
# See all containers
docker-compose -f docker-compose-preprod.yml ps

# View logs
docker-compose -f docker-compose-preprod.yml logs -f luna2-api

# Restart container
docker-compose -f docker-compose-preprod.yml restart luna2-api

# Execute command in container
docker-compose -f docker-compose-preprod.yml exec luna2-api bash

# Check resource usage
docker stats

# Stop all
docker-compose -f docker-compose-preprod.yml down

# Start all
docker-compose -f docker-compose-preprod.yml up -d
```


## When to Use Portainer vs CLI

| Task | Portainer | CLI |
|------|-----------|-----|
| Monitor containers | ✅ Visual | ✅ Quick |
| View logs | ✅ Easy | ✅ tail -f |
| Restart container | ✅ 1 click | ✓ 1 command |
| View resource usage | ✅ Real-time | ✓ docker stats |
| Execute commands | ✅ Terminal tab | ✅ docker exec |
| Backup database | ✓ Complex | ✅ mysqldump |
| Deploy new image | ✓ Manual | ✅ docker-compose |
| Multi-server mgmt | ✅ Perfect | ✓ Need scripts |

**Recommendation:** Use Portainer for monitoring, CLI for scripting and backups.
