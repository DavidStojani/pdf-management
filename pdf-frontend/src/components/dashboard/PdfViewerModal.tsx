import { useEffect, useCallback, useState } from "react";
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
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [loadingPdf, setLoadingPdf] = useState(false);

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

  useEffect(() => {
    if (!doc) {
      if (pdfUrl) {
        URL.revokeObjectURL(pdfUrl);
        setPdfUrl(null);
      }
      return;
    }

    let cancelled = false;
    setLoadingPdf(true);
    api
      .downloadDocument(doc.id)
      .then((blob) => {
        if (cancelled) return;
        const url = URL.createObjectURL(blob);
        setPdfUrl((prev) => {
          if (prev) URL.revokeObjectURL(prev);
          return url;
        });
      })
      .catch((err: any) => {
        if (!cancelled) {
          toast.error(err.message || "Failed to load preview");
        }
      })
      .finally(() => {
        if (!cancelled) setLoadingPdf(false);
      });

    return () => {
      cancelled = true;
    };
  }, [doc]);

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
        {loadingPdf ? (
          <div className="flex h-full items-center justify-center gap-2 text-muted-foreground">
            <Loader2 className="h-5 w-5 animate-spin" />
            <span>Loading preview…</span>
          </div>
        ) : pdfUrl ? (
          <iframe
            src={pdfUrl}
            className="h-full w-full border-0"
            title={doc.title}
          />
        ) : (
          <div className="flex h-full items-center justify-center text-muted-foreground">
            Preview unavailable
          </div>
        )}
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
      <DialogContent
        showClose={false}
        className="flex h-[85vh] max-w-4xl flex-col overflow-hidden p-0"
      >
        {viewer}
      </DialogContent>
    </Dialog>
  );
};

export default PdfViewerModal;
