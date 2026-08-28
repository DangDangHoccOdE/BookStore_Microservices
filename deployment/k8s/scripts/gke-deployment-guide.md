1) Tạo cluster trên GKE:
  gcloud container clusters create bookstore-cluster \
  --zone asia-southeast1-a \
  --num-nodes 2 \
  --machine-type e2-medium \
  --disk-type=pd-standard \
  --disk-size=30

2) Connect tới nó và kiểm tra kết nối
  gcloud container clusters get-credentials bookstore-cluster --zone asia-southeast1-a 
  kubectl get nodes

3) Triển khai Nginx Ingress Controller
    # 1. Thêm kho lưu trữ Helm của Ingress Nginx
    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx || true
    helm repo update
    
    # 2. Cài đặt Ingress Nginx Controller
    helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
    --namespace ingress-nginx --create-namespace \
    --set controller.service.type=LoadBalancer

4) Cài đặt ArgoCD (Đăng ký CRD "Application")
    # 1. Tạo Namespace cho ArgoCD
    kubectl create namespace argocd
    # 2. Cài đặt ArgoCD và các Custom Resource Definitions (CRDs) liên quan
   kubectl apply --server-side --force-conflicts -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
    # 3. Chờ cho ArgoCD khởi động xong hoàn toàn
    kubectl rollout status deployment/argocd-server -n argocd --timeout=150s

5) Triển khai Hạ tầng và Ứng dụng bằng GitOps
    # 1. Triển khai Hạ tầng (Postgres, Redis, RabbitMQ, Kafka, Keycloak)
    kubectl apply -f deployment/k8s/bookstore-infra-gitops.yaml
    # 2. Triển khai 6 Microservices ứng dụng
    kubectl apply -f deployment/k8s/bookstore-app-gitops.yaml

6) Cấu hình DNS giả lập (hosts) để kiểm thử
    # 1. Lấy External IP của Ingress Controller
   kubectl get svc ingress-nginx-controller -n ingress-nginx
   # 2. Cập nhật file hosts trên máy tính của bạn
    <EXTERNAL_IP_CUA_GKE> bookstore.local keycloak.local

   Mở trình duyệt truy cập: http://bookstore.local để sử dụng ứng dụng.
   Truy cập: http://keycloak.local để quản trị danh mục xác thực Keycloak.

7) Dọn dẹp tài nguyên (Xóa cụm để tránh phát sinh chi phí)
   gcloud container clusters delete bookstore-cluster --zone asia-southeast1-a --quiet
