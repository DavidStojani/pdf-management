const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const defaultMessageByStatus: Record<number, string> = {
  400: "Bad request",
  401: "Unauthorized",
  403: "Forbidden",
  404: "Not found",
  413: "Payload too large",
  500: "Internal server error",
  503: "Service unavailable",
};

function extractErrorMessage(errorBody: string, fallback: string): string {
  try {
    const parsed = JSON.parse(errorBody);
    return parsed.message || parsed.error || fallback;
  } catch {
    return errorBody || fallback;
  }
}

function formatHttpErrorMessage(status: number, message: string): string {
  return `HTTP ${status}: ${message}`;
}

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
      const errorBody = await response.text().catch(() => "");
      const defaultMessage = defaultMessageByStatus[401];
      const parsedMessage = extractErrorMessage(errorBody, defaultMessage);

      if (token) {
        this.clearToken();
        window.dispatchEvent(new CustomEvent("auth:expired"));
        throw new ApiError(formatHttpErrorMessage(401, "Session expired. Please log in again."), 401);
      }

      throw new ApiError(formatHttpErrorMessage(401, parsedMessage), 401);
    }

    if (!response.ok) {
      const errorBody = await response.text().catch(() => "");
      const fallback = defaultMessageByStatus[response.status] || `Request failed with status ${response.status}`;
      const message = extractErrorMessage(errorBody, fallback);
      throw new ApiError(formatHttpErrorMessage(response.status, message), response.status);
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

export function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

export interface DocumentItem {
  id: string;
  title: string;
  isFavourite?: boolean;
}

export const api = new ApiClient(API_BASE_URL);
