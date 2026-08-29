# HƯỚNG DẪN TRIỂN KHAI BOOKSTORE MICROSERVICES TRÊN GKE (GITOPS)

## Các bước triển khai từ đầu:

### 1) Tạo cluster trên GKE:
```bash
gcloud container clusters create bookstore-cluster \
--zone asia-southeast1-a \
--num-nodes 3 \
--machine-type e2-medium \
--disk-type=pd-standard \
--disk-size=30
```
> **Lưu ý:** Khuyến nghị dùng ít nhất **3 nodes** (`--num-nodes 3`) để đảm bảo đủ RAM/CPU chạy toàn bộ 12 dịch vụ ổn định.

### 2) Kết nối và kiểm tra cụm:
```bash
gcloud container clusters get-credentials bookstore-cluster --zone asia-southeast1-a 
kubectl get nodes
```

### 3) Triển khai Nginx Ingress Controller:
```bash
# 1. Thêm kho lưu trữ Helm của Ingress Nginx
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx || true
helm repo update

# 2. Cài đặt Ingress Nginx Controller
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
--namespace ingress-nginx --create-namespace \
--set controller.service.type=LoadBalancer
```

### 4) Cấu hình định tuyến mạng nội bộ (BƯỚC QUAN TRỌNG - THIẾU BƯỚC NÀY SẼ BỊ LỖI KẾT NỐI KEYCLOAK):
Vì các microservice gọi API Keycloak nội bộ qua tên miền giả lập `http://keycloak.local`, bạn cần trỏ IP định tuyến về Ingress Controller của cụm mới:
1. Lấy **CLUSTER-IP** (IP nội bộ) của Ingress Nginx Controller:
   ```bash
   kubectl get svc ingress-nginx-controller -n ingress-nginx
   ```
2. Mở file `deployment/k8s/bookstore-app/values-prod.yaml`, tìm khóa `global.hostAliases.ip` và cập nhật giá trị IP này bằng CLUSTER-IP vừa lấy ở trên.
3. Commit và push thay đổi lên Git trước khi triển khai ứng dụng.

### 5) Cài đặt ArgoCD (Đăng ký CRD "Application"):
```bash
# 1. Tạo Namespace cho ArgoCD
kubectl create namespace argocd

# 2. Cài đặt ArgoCD và các Custom Resource Definitions (CRDs) liên quan
kubectl apply --server-side --force-conflicts -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 3. Chờ cho ArgoCD khởi động xong hoàn toàn
kubectl rollout status deployment/argocd-server -n argocd --timeout=150s
```

### 6) Triển khai Hạ tầng và Ứng dụng bằng GitOps:
```bash
# 1. Triển khai Hạ tầng (Postgres, Redis, RabbitMQ, Kafka, Keycloak)
kubectl apply -f deployment/k8s/bookstore-infra-gitops.yaml

# 2. Triển khai 6 Microservices ứng dụng
kubectl apply -f deployment/k8s/bookstore-app-gitops.yaml
```

### 7) Cấu hình DNS giả lập trên máy cá nhân để kiểm thử:
1. Lấy **EXTERNAL-IP** (IP công khai) của Ingress Controller:
   ```bash
   kubectl get svc ingress-nginx-controller -n ingress-nginx
   ```
2. Thêm dòng sau vào file `hosts` của máy tính cá nhân (Windows: `C:\Windows\System32\drivers\etc\hosts`):
   ```text
   <EXTERNAL-IP_CỦA_INGRESS> bookstore.local keycloak.local
   ```
3. Truy cập hệ thống:
   - Giao diện ứng dụng: `http://bookstore.local`
   - Quản trị Keycloak: `http://keycloak.local`
   - Xem giao diện ArgoCD (mở port-forward ở terminal):
     ```bash
     kubectl port-forward svc/argocd-server -n argocd 8080:443
     ```
     Truy cập `https://localhost:8080` (Tài khoản: `admin` / Password mặc định lấy từ secret argocd-initial-admin-secret).

---

## 🛠️ HƯỚNG DẪN SỬA LỖI (TROUBLESHOOTING)

Trong quá trình triển khai trên môi trường thật, hãy lưu ý các điểm mấu chốt sau để tránh lỗi hệ thống:

### 1. Lỗi kẹt tài nguyên khi nâng cấp (Rolling Update Deadlock):
* **Triệu chứng:** Khi update code, Pod mới ở trạng thái `Pending` (thiếu CPU/RAM) còn Pod cũ không chịu tắt, cụm bị treo.
* **Cách sửa:** Chạy lệnh hạ số lượng replica của ReplicaSet cũ về 0 để giải phóng RAM/CPU:
  ```bash
  kubectl scale rs <ten-replicaset-cu> --replicas=0 -n bookstore-prod
  ```
  Hoặc xóa Pod cũ để kích hoạt tạo Pod mới ngay lập tức:
  ```bash
  kubectl delete pod <ten-pod> -n bookstore-prod
  ```

### 2. Lỗi quá thời hạn kiểm tra sức khỏe RabbitMQ (Probe Timeout):
* **Triệu chứng:** RabbitMQ chạy thành công nhưng luôn ở trạng thái `0/1 READY` và có cảnh báo `timed out after 1s`.
* **Lý do:** Công cụ kiểm tra `rabbitmq-diagnostics` tốn hơn 1 giây để khởi động trên GKE CPU thấp (`50m`).
* **Cách sửa:** Đảm bảo cấu hình `readinessProbe` trong `rabbitmq.yaml` có khai báo `timeoutSeconds: 10`.

### 3. Lỗi 401 Unauthorized tại các Actuator Endpoint:
* **Triệu chứng:** Các dịch vụ Java chạy lên bị sập vòng lặp (CrashLoopBackOff) do liveness/readiness probe trả về mã HTTP `401`.
* **Lý do:** Spring Security chặn đường dẫn con `/actuator/health/liveness` và `/actuator/health/readiness`.
* **Cách sửa:** Code Java Spring Security cấu hình permitAll cho wildcard đường dẫn rộng: `"/actuator/**"`.

### 4. Lỗi 403 Permission Denied khi chạy GitHub Actions CI/CD:
* **Triệu chứng:** GitHub Actions chạy thành công bước build nhưng sập ở bước `git push` cập nhật tag ảnh.
* **Cách sửa:** Khai báo quyền ghi cho file workflow `.yml`:
  ```yaml
  permissions:
    contents: write
  ```

---

## Xóa cụm để tránh phát sinh chi phí:
```bash
gcloud container clusters delete bookstore-cluster --zone asia-southeast1-a --quiet
```
