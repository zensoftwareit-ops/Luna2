#!/bin/bash
# Deep dive into Tomcat deployment status

DOMAIN="${1:-5upemke9qv6ov6-app.gestionaleluna.it}"
CONTAINER="luna2-$DOMAIN"

echo "=== TOMCAT DEPLOYMENT DEBUG ==="
echo

echo "1. Tutti i log Tomcat disponibili:"
docker exec $CONTAINER ls -lh /usr/local/tomcat/logs/ 2>/dev/null
echo

echo "2. Catalina log (deployment info):"
docker exec $CONTAINER cat /usr/local/tomcat/logs/catalina.*.log 2>/dev/null | tail -n 100 || echo "No catalina log"
echo

echo "3. Localhost log (webapp errors):"
docker exec $CONTAINER cat /usr/local/tomcat/logs/localhost.*.log 2>/dev/null | tail -n 50 || echo "No localhost log"
echo

echo "4. ROOT webapp log:"
docker exec $CONTAINER cat /usr/local/tomcat/logs/localhost_access_log.*.txt 2>/dev/null | tail -n 20 || echo "No access log"
echo

echo "5. WEB-INF/web.xml presente?"
docker exec $CONTAINER cat /usr/local/tomcat/webapps/ROOT/WEB-INF/web.xml 2>/dev/null | head -n 50 || echo "✗ web.xml non trovato o non leggibile"
echo

echo "6. WEB-INF/classes:"
docker exec $CONTAINER find /usr/local/tomcat/webapps/ROOT/WEB-INF/classes -type f 2>/dev/null | head -n 20 || echo "✗ No classes"
echo

echo "7. WEB-INF/lib (JAR libraries):"
docker exec $CONTAINER ls -lh /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/ 2>/dev/null | head -n 20 || echo "✗ No libraries"
echo

echo "8. Check WAR integrity:"
docker exec $CONTAINER file /usr/local/tomcat/webapps/ROOT.war
docker exec $CONTAINER unzip -l /usr/local/tomcat/webapps/ROOT.war | head -n 30
echo

echo "9. Tomcat deployment status (manager):"
docker exec $CONTAINER curl -s http://localhost:8080/manager/text/list 2>/dev/null || echo "Manager not available"
echo

echo "10. Java process status:"
docker exec $CONTAINER ps aux | grep -i java
