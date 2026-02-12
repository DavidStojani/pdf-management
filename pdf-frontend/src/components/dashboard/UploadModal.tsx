import { useState, useRef } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle } from "@/components/ui/drawer";
import { Button } from "@/components/ui/button";
import { FileUp, Camera, X, Loader2 } from "lucide-react";
import { api, getErrorMessage } from "@/lib/api";
import { toast } from "sonner";
import { useIsMobile } from "@/hooks/use-mobile";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

const UploadModal = ({ open, onOpenChange, onSuccess }: Props) => {
  const isMobile = useIsMobile();
  const [mode, setMode] = useState<"choose" | "camera">("choose");
  const [cameraFiles, setCameraFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const reset = () => {
    setMode("choose");
    setCameraFiles([]);
    setUploading(false);
  };

  const handleClose = (v: boolean) => {
    if (!v) reset();
    onOpenChange(v);
  };

  const handleDeviceUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      await api.uploadFile(file);
      toast.success("File uploaded successfully!");
      onSuccess();
      handleClose(false);
    } catch (err: any) {
      toast.error(getErrorMessage(err, "Upload failed"));
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleCameraCapture = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    setCameraFiles((prev) => [...prev, ...files]);
    if (cameraInputRef.current) cameraInputRef.current.value = "";
  };

  const removeCameraFile = (index: number) => {
    setCameraFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleCameraUpload = async () => {
    if (cameraFiles.length === 0) return;
    setUploading(true);
    try {
      await api.uploadCameraImages(cameraFiles);
      toast.success("Images uploaded successfully!");
      onSuccess();
      handleClose(false);
    } catch (err: any) {
      toast.error(getErrorMessage(err, "Upload failed"));
    } finally {
      setUploading(false);
    }
  };

  const content = (
    <div className="space-y-4 pb-2">
      {uploading && (
        <div className="flex items-center justify-center gap-2 py-8 text-primary">
          <Loader2 className="h-6 w-6 animate-spin-slow" />
          <span className="text-sm font-medium">Uploading…</span>
        </div>
      )}

      {!uploading && mode === "choose" && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <button
            onClick={() => fileInputRef.current?.click()}
            className="glass-card flex flex-col items-center gap-3 p-6 transition-all hover:vault-shadow-md hover:border-primary/30"
          >
            <FileUp className="h-10 w-10 text-primary" />
            <div className="text-center">
              <p className="font-medium text-foreground">From device</p>
              <p className="text-xs text-muted-foreground">Select a PDF file</p>
            </div>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="application/pdf"
            className="hidden"
            onChange={handleDeviceUpload}
          />

          <button
            onClick={() => {
              setMode("camera");
              setTimeout(() => cameraInputRef.current?.click(), 100);
            }}
            className="glass-card flex flex-col items-center gap-3 p-6 transition-all hover:vault-shadow-md hover:border-primary/30"
          >
            <Camera className="h-10 w-10 text-primary" />
            <div className="text-center">
              <p className="font-medium text-foreground">From camera</p>
              <p className="text-xs text-muted-foreground">Capture multiple images</p>
            </div>
          </button>
        </div>
      )}

      {!uploading && mode === "camera" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <Button variant="ghost" size="sm" onClick={() => setMode("choose")}>
              ← Back
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => cameraInputRef.current?.click()}
            >
              <Camera className="mr-1.5 h-4 w-4" />
              Add more
            </Button>
          </div>

          <input
            ref={cameraInputRef}
            type="file"
            accept="image/*"
            capture="environment"
            multiple
            className="hidden"
            onChange={handleCameraCapture}
          />

          {cameraFiles.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              No images captured yet. Tap "Add more" to start.
            </p>
          ) : (
            <div className="grid grid-cols-3 gap-2">
              {cameraFiles.map((f, i) => (
                <div key={i} className="relative aspect-square overflow-hidden rounded-lg border border-border">
                  <img
                    src={URL.createObjectURL(f)}
                    alt={f.name}
                    className="h-full w-full object-cover"
                  />
                  <button
                    onClick={() => removeCameraFile(i)}
                    className="absolute right-1 top-1 rounded-full bg-foreground/70 p-0.5 text-background transition-colors hover:bg-foreground"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
              ))}
            </div>
          )}

          {cameraFiles.length > 0 && (
            <Button className="w-full" onClick={handleCameraUpload}>
              Upload {cameraFiles.length} image{cameraFiles.length > 1 ? "s" : ""}
            </Button>
          )}
        </div>
      )}
    </div>
  );

  if (isMobile) {
    return (
      <Drawer open={open} onOpenChange={handleClose}>
        <DrawerContent>
          <DrawerHeader>
            <DrawerTitle>Upload Document</DrawerTitle>
          </DrawerHeader>
          <div className="px-4 pb-6">{content}</div>
        </DrawerContent>
      </Drawer>
    );
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Upload Document</DialogTitle>
        </DialogHeader>
        {content}
      </DialogContent>
    </Dialog>
  );
};

export default UploadModal;
