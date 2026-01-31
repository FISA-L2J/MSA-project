#!/bin/bash

# 1. Define Environment Variables for Docker & App
# You can change the password below or set DB_PASSWORD env var before running this script
DB_PASSWORD=${DB_PASSWORD:-"your_password_change_me"}

echo "📄 Creating .env file for Docker Compose..."
cat <<EOF > .env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=${DB_PASSWORD}
POSTGRES_DB=msa_db
REDIS_PORT=6379
ZIPKIN_PORT=9411
POSTGRES_PORT=5432
EOF

# 2. Generate Local RSA Keys
echo "🔑 Generating Local RSA Keys..."
openssl genrsa -out local_private.pem 2048
openssl rsa -in local_private.pem -pubout -out local_public.pem

# Extract key content removing headers/footers/newlines for Env Var usage
PRIVATE_KEY_CONTENT=$(cat local_private.pem)
PUBLIC_KEY_CONTENT=$(cat local_public.pem)

# 3. Append Keys to .env.local for Applications
echo "📄 Creating .env.local file for Spring Boot Apps..."
cp .env .env.local
cat <<EOF >> .env.local
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=${DB_PASSWORD}
export POSTGRES_DB=msa_db
# JWT Config (RS256)
export JWT_PRIVATE_KEY="$PRIVATE_KEY_CONTENT"
export JWT_PUBLIC_KEY="$PUBLIC_KEY_CONTENT"
EOF

# 4. Start Infrastructure (DB, Redis, Zipkin)
echo "🐳 Starting Infrastructure..."
docker-compose up -d postgres redis zipkin

# Clean up PEM files
rm local_private.pem local_public.pem

echo "✅ Environment Setup Complete!"
echo "============================================"
echo "⚠️ IMPORTANT: If DB auth fails, run 'docker-compose down -v' and retry this script!"
echo "============================================"
echo "To run services locally, open separate terminals and run:"
echo ""
echo "[Terminal 1: Auth Service]"
echo "source .env.local && ./gradlew :auth-service:bootRun"
echo ""
echo "[Terminal 2: Account Service]"
echo "source .env.local && ./gradlew :account-service:bootRun"
echo ""
echo "[Terminal 3: Transaction Service]"
echo "source .env.local && ./gradlew :transaction-service:bootRun"
echo "============================================"
