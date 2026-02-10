import { useEffect, useCallback } from "react";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { Drawer, DrawerContent } from "@/components/ui/drawer";
import { Button } from "@/components/ui/button";
import { X, Download, Loader2 } from "lucide-react";
import { DocumentItem, api } from "@/lib/api";
import { toast } from "sonner";
import { useIsMobile } from "@/hooks/use-mobile";
import FavouriteButton from "./FavouriteButton";

interface Props {
  document: DocumentItem | null;
  onClose: () => void;
  onFavouriteToggle: (id: string, isFav: boolean) => void;
}

const PdfViewerModal = ({ document: doc, onClose, onFavouriteToggle }: Props) => {
  const isMobile = useIsMobile();

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    },
    [onClose]
  );

  useEffect(() => {
    if (doc) {
      window.addEventListener("keydown", handleKeyDown);
      return () => window.removeEventListener("keydown", handleKeyDown);
    }
  }, [doc, handleKeyDown]);

  const handleDownload = async () => {
    if (!doc) return;
    try {
      const blob = await api.downloadDocument(doc.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${doc.title}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success("Download started");
    } catch (err: any) {
      toast.error(err.message || "Download failed");
    }
  };

  if (!doc) return null;

  const pdfUrl = api.getDownloadUrl(doc.id);

  const viewer = (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <h3 className="min-w-0 flex-1 truncate text-sm font-semibold text-foreground">
          {doc.title}
        </h3>
        <div className="flex items-center gap-1">
          <FavouriteButton
            docId={doc.id}
            isFavourite={doc.isFavourite ?? false}
            onToggle={onFavouriteToggle}
          />
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handleDownload}>
            <Download className="h-4 w-4" />
          </Button>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* PDF iframe */}
      <div className="flex-1 bg-muted">
        <iframe
          src={pdfUrl}
          className="h-full w-full border-0"
          title={doc.title}
        />
      </div>
    </div>
  );

  if (isMobile) {
    return (
      <Drawer open={!!doc} onOpenChange={(v) => !v && onClose()}>
        <DrawerContent className="h-[95vh]">
          {viewer}
        </DrawerContent>
      </Drawer>
    );
  }

  return (
    <Dialog open={!!doc} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="flex h-[85vh] max-w-4xl flex-col overflow-hidden p-0">
        {viewer}
      </DialogContent>
    </Dialog>
  );
};

export default PdfViewerModal;
