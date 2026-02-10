import { FileText } from "lucide-react";
import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";

const PublicLayout = () => {
  const { isAuthenticated } = useAuth();

  if (isAuthenticated) return <Navigate to="/app" replace />;

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="flex items-center justify-center py-8">
        <div className="flex items-center gap-2.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary vault-shadow">
            <FileText className="h-5 w-5 text-primary-foreground" />
          </div>
          <span className="text-xl font-bold text-foreground">PDF Vault</span>
        </div>
      </header>

      <main className="flex flex-1 items-center justify-center px-4 pb-12">
        <Outlet />
      </main>

      <footer className="py-4 text-center text-xs text-muted-foreground">
        © {new Date().getFullYear()} PDF Vault. Secure document storage.
      </footer>
    </div>
  );
};

export default PublicLayout;
