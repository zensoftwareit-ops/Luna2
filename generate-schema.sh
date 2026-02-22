#!/bin/bash
# Generate Hibernate DDL Schema

cd "$(dirname "$0")"

HIBERNATE_VERSION="5.6.15.Final"
H2_VERSION="2.1.214"
LOG4J_VERSION="2.20.0"

# Create temp lib directory
mkdir -p temp-lib
cd temp-lib

# Download Hibernate JAR if not exists
if [ ! -f "hibernate-core-$HIBERNATE_VERSION.jar" ]; then
    echo "Downloading Hibernate Core..."
    wget -q "https://repo1.maven.org/maven2/org/hibernate/hibernate-core/$HIBERNATE_VERSION/hibernate-core-$HIBERNATE_VERSION.jar"
fi

if [ ! -f "h2-$H2_VERSION.jar" ]; then
    echo "Downloading H2 Database..."
    wget -q "https://repo1.maven.org/maven2/com/h2database/h2/$H2_VERSION/h2-$H2_VERSION.jar"
fi

cd ..

# Generate schema
echo "Generating SQL schema..."
java -cp "target/classes:temp-lib/*" \
    org.hibernate.tool.hbm2ddl.SchemaExport \
    --config=src/main/resources/hibernate.cfg.xml \
    --output=src/main/resources/schema.sql \
    --create

if [ -f "src/main/resources/schema.sql" ]; then
    echo "✓ Schema generated: src/main/resources/schema.sql"
    echo ""
    echo "--- Generated DDL ---"
    head -50 src/main/resources/schema.sql
else
    echo "✗ Schema generation failed"
fi
