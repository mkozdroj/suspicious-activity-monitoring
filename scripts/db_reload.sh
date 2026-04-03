#!/bin/bash

# Script to drop and reload database from dump file

DB_NAME="your_database_name"
DUMP_FILE="path/to/your/dumpfile.sql"

# Drop the database if it exists
if [ -d "$DB_NAME" ]; then
    echo "Dropping existing database..."
    dropdb $DB_NAME
fi

# Create a new database
echo "Creating new database..."
createdb $DB_NAME

# Reload the database from the dump file
echo "Reloading database from dump file..."
psql $DB_NAME < $DUMP_FILE

echo "Database reloaded successfully!"