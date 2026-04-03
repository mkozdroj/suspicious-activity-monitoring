#!/bin/bash

# db_dump.sh - Full schema and data dump

# Set database credentials
db_user='your_username'
db_password='your_password'
db_name='your_database_name'
host='localhost'

# Create a timestamp for the dump file name
timestamp=$(date +'%Y-%m-%d_%H-%M-%S')

dump_file="db_dump_$timestamp.sql"

# Dump schema and data to the file
mysqldump --host=$host --user=$db_user --password=$db_password --databases $db_name --routines --triggers --events > $dump_file

# Print success message
if [ $? -eq 0 ]; then
    echo "Database dump successful! Dump file: $dump_file"
else
    echo "Database dump failed!"
}
