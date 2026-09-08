# Build in ACR and roll out joyway-backend-prod + joyway-frontend-prod
# Usage: .\scripts\deploy-azure-prod.ps1 [-Tag prod-2026-09-02]

param(
    [string]$Tag = "prod-$(Get-Date -Format 'yyyyMMdd-HHmm')"
)

$ErrorActionPreference = "Stop"
$Rg = "rg-joyway-prod"
$Acr = "joywayprodacr"
$BackendApp = "joyway-backend-prod"
$FrontendApp = "joyway-frontend-prod"
$ApiUrl = "https://api.joyway.in"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

Write-Host "Deploy tag: $Tag"

$RzpKey = az containerapp show -g $Rg -n $BackendApp `
    --query "properties.template.containers[0].env[?name=='RAZORPAY_KEY_ID'].value" -o tsv
if (-not $RzpKey) { throw "RAZORPAY_KEY_ID not found on backend container app." }

Write-Host "Building backend image in ACR..."
az acr build --registry $Acr `
    --image "joyway-backend:$Tag" `
    --file "$RepoRoot/backend/Dockerfile" `
    "$RepoRoot/backend"

Write-Host "Building frontend image in ACR..."
az acr build --registry $Acr `
    --image "joyway-frontend:$Tag" `
    --build-arg "NEXT_PUBLIC_API_BASE_URL=$ApiUrl" `
    --build-arg "NEXT_PUBLIC_RAZORPAY_KEY_ID=$RzpKey" `
    --file "$RepoRoot/frontend/Dockerfile" `
    "$RepoRoot/frontend"

Write-Host "Updating backend container app..."
az containerapp update -g $Rg -n $BackendApp `
    --image "$Acr.azurecr.io/joyway-backend:$Tag"

Write-Host "Updating frontend container app..."
az containerapp update -g $Rg -n $FrontendApp `
    --image "$Acr.azurecr.io/joyway-frontend:$Tag"

Write-Host "Done. Verify:"
Write-Host "  $ApiUrl/api/health/ready"
Write-Host "  $ApiUrl/api/packages"
Write-Host "  https://joyway.in"
