#!/bin/bash
 
# Script to set up AmazonInternalMCPServer for use with the Q CLI
# Created: March 31, 2025
 
set -e
 
echo "Setting up AmazonInternalMCPServer for Amazon Q CLI..."
 
# Step 1: Install the MCP server using toolbox
echo "Step 1: Installing amzn-mcp using toolbox..."
if ! command -v toolbox &> /dev/null; then
    echo "Error: toolbox command not found. Please install toolbox first."
    exit 1
fi
 
echo "Adding MCP registry to toolbox..."
toolbox registry add s3://amzn-mcp-prod-registry-bucket-us-west-2/tools.json
 
echo "Installing amzn-mcp..."
toolbox install amzn-mcp
 
# Step 2: Create the MCP configuration file
echo "Step 2: Creating MCP configuration file..."
 
# Create global MCP configuration directory
GLOBAL_CONFIG_DIR="$HOME/.aws/amazonq"
GLOBAL_CONFIG_FILE="$GLOBAL_CONFIG_DIR/mcp.json"
 
# Create the directory if it doesn't exist
mkdir -p "$GLOBAL_CONFIG_DIR"
 
# Check if file exists
if [ -f "$GLOBAL_CONFIG_FILE" ]; then
    echo "Existing configuration found at $GLOBAL_CONFIG_FILE"
    echo "Would you like to overwrite it? (y/n)"
    read -r overwrite
    if [[ "$overwrite" != "y" && "$overwrite" != "Y" ]]; then
        echo "Skipping configuration update."
        echo "Setup complete!"
        exit 0
    fi
fi
 
# Create configuration with the specified content
cat > "$GLOBAL_CONFIG_FILE" << EOF
{
  "mcpServers": {
    "amazon-internal-mcp-server": {
      "command": "amzn-mcp",
      "args": [],
      "env": {}
    }
  }
}
EOF
 
echo "Created MCP configuration at $GLOBAL_CONFIG_FILE"
echo ""
echo "Setup complete!"
echo ""
echo "You can now use Amazon Q CLI with the internal MCP server."
