#!/bin/bash
set -e

NAMESPACE="bookstore-local"
echo "Creating Namespace: $NAMESPACE..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

echo "========= 1. Cài đặt Nginx Ingress Controller (Local) ========="
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx || true
helm repo update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.hostPort.enabled=true

echo "========= 2. Cài đặt Cert-Manager (Local) ========="
helm repo add jetstack https://charts.jetstack.io || true
helm repo update
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --version v1.13.0 \
  --set installCRDs=true

echo "Waiting for Cert-Manager to be ready..."
kubectl rollout status deployment/cert-manager-webhook -n cert-manager --timeout=120s

GIT_SHA=$(git rev-parse --short HEAD)
echo "Resolved local deploy tag (Git SHA): $GIT_SHA"

echo "========= 3. Deploy Infrastructure ========="
helm upgrade --install bookstore-infra ./deployment/k8s/infra-chart \
  -f ./deployment/k8s/infra-chart/values-local.yaml \
  --namespace $NAMESPACE

echo "========= 4. Deploy Bookstore Applications ========="
helm upgrade --install bookstore-app ./deployment/k8s/bookstore-app \
  -f ./deployment/k8s/bookstore-app/values-local.yaml \
  --namespace $NAMESPACE \
  --set global.imageTag="latest"

echo "=========================================================="
echo "DEPLOY LOCAL THÀNH CÔNG!"
echo "Tag ảnh hiện tại: $GIT_SHA"
echo "Hãy kiểm tra các pod: kubectl get pods -n $NAMESPACE"
echo "Khai báo file hosts: 127.0.0.1 bookstore.local keycloak.local"
echo "=========================================================="