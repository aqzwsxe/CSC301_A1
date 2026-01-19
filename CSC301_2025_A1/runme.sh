#!/bin/bash

#First line: tell the computer to use the Bash shell to execute these commands
# Configuration
CONFIG="config.json"
OUT_DIR="compiled"
SRC_DIR="src"

# Function to compile a service
compile_service(){
#   Capture the name of the service
    local  service=$1
    echo "Compiling $service..."
#   Create the directory structure; -p: if the parent directory does not exist
#   create as well
    mkdir -p "$OUT_DIR/$service"
#   The java Compiler. The -d flag tells it to put the resulting .class into the compiled directory;
#   It compiles all Java files for that specific service at once
    javac -d "$OUT_DIR" "$SRC_DIR/$service"/*.java
}

case "$1" in
    -c)
      echo  "Cleaning and Compiling all service"
      rm -rf "$OUT_DIR"
      mkdir -p "$OUT_DIR"
      compile_service "UserService"
      # compile_service "ProductService"
      # compile_service "OrderService"
      # compile_service "ISCS"
      echo  "Done"
      ;;

    -u)
      echo "Starting User Service"
      java -cp "$OUT_DIR$" UserService.UserService "$CONFIG"
      ;;

    -p)
      echo "Starting Product Service"
      java -cp "$OUT_DIR" ProductService.ProductService "$CONFIG"
      ;;
    -i)
          echo "Starting Inter-Service Communication Service (ISCS)"
          java -cp "$OUT_DIR" ISCS.ISCS "$CONFIG"
          ;;
    -o)
      echo "Starting Order Service"
      java -cp "$OUT_DIR" OrderService.OrderService "$CONFIG"
      ;;

    -w)
      # Check if the workload file was provided
      if [ -z "$2" ]; then
          echo "Error: Please provide a workload file"
      fi
      echo "Starting Workload Parser with file: $2"
      ;;
esac