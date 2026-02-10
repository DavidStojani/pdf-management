const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private getToken(): string | null {
    return localStorage.getItem("vault_token");
  }

  private setToken(token: string): void {
    localStorage.setItem("vault_token", token);
  }

  clearToken(): void {
    localStorage.removeItem("vault_token");
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = this.getToken();
    const headers: Record<string, string> = {
      ...(options.headers as Record<string, string>),
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    if (!(options.body instanceof FormData)) {
      headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      this.clearToken();
      window.dispatchEvent(new CustomEvent("auth:expired"));
      throw new ApiError("Session expired. Please log in again.", 401);
    }

    if (!response.ok) {
      const errorBody = await response.text().catch(() => "");
      let message = `Request failed with status ${response.status}`;
      try {
        const parsed = JSON.parse(errorBody);
        message = parsed.message || parsed.error || message;
      } catch {
        if (errorBody) message = errorBody;
      }
      throw new ApiError(message, response.status);
    }

    const contentType = response.headers.get("content-type");
    if (contentType?.includes("application/json")) {
      return response.json();
    }
    return response as unknown as T;
  }

  // Auth
  async login(email: string, password: string): Promise<{ token: string }> {
    const result = await this.request<{ jwtToken: string }>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    this.setToken(result.jwtToken);
    return { token: result.jwtToken };
  }

  async register(
    username: string,
    email: string,
    password: string,
    confirmPassword: string
  ): Promise<{ token: string }> {
    const result = await this.request<{ jwtToken: string }>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, email, password, confirmPassword }),
    });
    this.setToken(result.jwtToken);
    return { token: result.jwtToken };
  }

  async forgotPassword(email: string): Promise<void> {
    await this.request("/api/auth/password-reset-request", {
      method: "POST",
      body: JSON.stringify({ email }),
    });
  }

  // Documents
  async searchDocuments(query: string): Promise<DocumentItem[]> {
    return this.request<DocumentItem[]>(
      `/api/documents/search?q=${encodeURIComponent(query)}`
    );
  }

  async getFavourites(): Promise<DocumentItem[]> {
    return this.request<DocumentItem[]>("/api/documents/favourites");
  }

  async uploadFile(file: File): Promise<void> {
    const formData = new FormData();
    formData.append("file", file);
    await this.request("/api/documents/upload", {
      method: "POST",
      body: formData,
    });
  }

  async uploadCameraImages(files: File[]): Promise<void> {
    const formData = new FormData();
    files.forEach((f) => formData.append("files[]", f));
    await this.request("/api/documents/upload/camera", {
      method: "POST",
      body: formData,
    });
  }

  getDownloadUrl(id: string): string {
    const token = this.getToken();
    return `${this.baseUrl}/api/documents/${id}/download${token ? `?token=${token}` : ""}`;
  }

  async downloadDocument(id: string): Promise<Blob> {
    const response = await this.request<Response>(
      `/api/documents/${id}/download`
    );
    return (response as unknown as Response).blob();
  }

  async addFavourite(id: string): Promise<void> {
    await this.request(`/api/documents/${id}/favourite`, { method: "POST" });
  }

  async removeFavourite(id: string): Promise<void> {
    await this.request(`/api/documents/${id}/favourite`, { method: "DELETE" });
  }
}

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export interface DocumentItem {
  id: string;
  title: string;
  isFavourite?: boolean;
}

export const api = new ApiClient(API_BASE_URL);
