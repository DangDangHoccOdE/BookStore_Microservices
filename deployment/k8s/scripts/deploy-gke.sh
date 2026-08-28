#!/bin/bash
set -e

# Khai báo cấu hình dự án
PROJECT_ID="fluent-observer-457907-r7" # Hãy thay bằng Project ID thực tế của bạn
ZONE="asia-southeast1-a"
CLUSTER_NAME="bookstore-cluster"
NAMESPACE="bookstore-prod"

echo "Configuring GCP Project & GKE Context..."
gcloud config set project $PROJECT_ID
gcloud container clusters get-credentials $CLUSTER_NAME --zone $ZONE

echo "Creating Namespace: $NAMESPACE..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

echo "========= 1. Cài đặt Nginx Ingress Controller (GKE) ========="
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx || true
helm repo update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.type=LoadBalancer

GIT_SHA=$(git rev-parse --short HEAD)
echo "Production Deploy Tag: $GIT_SHA"

echo "========= 2. Deploy Production Infrastructure (Postgres tự chạy trong GKE) ========="
helm upgrade --install bookstore-infra ./deployment/k8s/infra-chart \
  -f ./deployment/k8s/infra-chart/values-prod.yaml \
  --namespace $NAMESPACE

echo "========= 3. Deploy Production Bookstore Applications (HTTP Mode) ========="
helm upgrade --install bookstore-app ./deployment/k8s/bookstore-app \
  -f ./deployment/k8s/bookstore-app/values-prod.yaml \
  --namespace $NAMESPACE \
  --set global.imageTag="$GIT_SHA"

echo "=========================================================="
echo "DEPLOY PRODUCTION GKE HOÀN TẤT!"
echo "Tag ảnh hiện tại: $GIT_SHA"
echo "Bước 1: Lấy IP LoadBalancer bằng lệnh sau:"
echo "   kubectl get svc ingress-nginx-controller -n ingress-nginx"
echo "Bước 2: Cập nhật IP GKE đó vào file hosts của máy bạn:"
echo "   <IP_GKE> bookstore.local keycloak.local"
echo "Bước 3: Truy cập http://bookstore.local để test hệ thống."
echo "=========================================================="