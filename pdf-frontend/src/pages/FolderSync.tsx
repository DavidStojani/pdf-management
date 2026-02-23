import { useRef, useState } from "react";
import { FolderOpen, Loader2, CheckCircle2, XCircle, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { api, getErrorMessage } from "@/lib/api";
import { toast } from "sonner";

type FileStatus = "pending" | "uploading" | "done" | "error";

type FileEntry = {
  file: File;
  status: FileStatus;
  error?: string;
};

const StatusBadge = ({ status, error }: { status: FileStatus; error?: string }) => {
  if (status === "uploading") {
    return (
      <Badge variant="secondary" className="gap-1.5">
        <Loader2 className="h-3 w-3 animate-spin" />
        Uploading
      </Badge>
    );
  }
  if (status === "done") {
    return (
      <Badge variant="secondary" className="gap-1.5 bg-green-500/10 text-green-600 border-green-500/20">
        <CheckCircle2 className="h-3 w-3" />
        Done
      </Badge>
    );
  }
  if (status === "error") {
    return (
      <Badge variant="destructive" className="gap-1.5" title={error}>
        <XCircle className="h-3 w-3" />
        Error
      </Badge>
    );
  }
  return (
    <Badge variant="outline" className="text-muted-foreground">
      Pending
    </Badge>
  );
};

const FolderSync = () => {
  const inputRef = useRef<HTMLInputElement>(null);
  const [fileEntries, setFileEntries] = useState<FileEntry[]>([]);
  const [uploading, setUploading] = useState(false);

  const isPdf = (file: File) =>
    file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");

  const handleFolderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []).filter(isPdf);
    setFileEntries(files.map((file) => ({ file, status: "pending" })));
    if (e.target) e.target.value = "";
  };

  const updateStatus = (
    index: number,
    status: FileStatus,
    error?: string
  ) => {
    setFileEntries((prev) =>
      prev.map((entry, i) =>
        i === index ? { ...entry, status, error } : entry
      )
    );
  };

  const handleUploadAll = async () => {
    if (fileEntries.length === 0 || uploading) return;
    setUploading(true);

    let successCount = 0;
    let failCount = 0;

    for (let i = 0; i < fileEntries.length; i++) {
      if (fileEntries[i].status === "done") {
        successCount++;
        continue;
      }
      updateStatus(i, "uploading");
      try {
        await api.uploadFolderFile(fileEntries[i].file);
        updateStatus(i, "done");
        successCount++;
      } catch (err) {
        const message = getErrorMessage(err, "Upload failed");
        updateStatus(i, "error", message);
        failCount++;
      }
    }

    setUploading(false);

    if (failCount === 0) {
      toast.success(`${successCount} file${successCount !== 1 ? "s" : ""} uploaded successfully`);
    } else {
      toast.warning(`${successCount} uploaded, ${failCount} failed`);
    }
  };

  const handleClear = () => {
    setFileEntries([]);
  };

  const folderName = fileEntries[0]?.file.webkitRelativePath?.split("/")[0];
  const isbusy = uploading || fileEntries.some((e) => e.status === "uploading");
  const allDone = fileEntries.length > 0 && fileEntries.every((e) => e.status === "done");

  return (
    <div className="animate-fade-in space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-foreground">Folder Sync</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Upload all PDFs from a local folder at once.
        </p>
      </div>

      {/* Hidden folder input */}
      {/* @ts-ignore */}
      <input
        ref={inputRef}
        type="file"
        multiple
        // @ts-ignore
        webkitdirectory=""
        accept=".pdf,application/pdf"
        className="hidden"
        onChange={handleFolderChange}
      />

      <div className="glass-card vault-shadow-md space-y-5 p-6">
        {/* Folder picker */}
        <div className="flex flex-wrap items-center gap-3">
          <Button
            variant="outline"
            onClick={() => inputRef.current?.click()}
            disabled={isbusy}
            className="gap-2"
          >
            <FolderOpen className="h-4 w-4" />
            Select Folder
          </Button>

          {fileEntries.length > 0 && (
            <span className="text-sm text-muted-foreground">
              {folderName ? (
                <>
                  <span className="font-medium text-foreground">"{folderName}"</span>
                  {" • "}
                </>
              ) : null}
              {fileEntries.length} PDF file{fileEntries.length !== 1 ? "s" : ""}
            </span>
          )}
        </div>

        {/* File list */}
        {fileEntries.length > 0 && (
          <div className="rounded-lg border border-border divide-y divide-border overflow-hidden">
            {fileEntries.map((entry, i) => (
              <div
                key={i}
                className="flex items-center justify-between gap-2 px-4 py-2.5 text-sm"
              >
                <span className="truncate text-foreground min-w-0">
                  {entry.file.name}
                </span>
                <div className="flex-shrink-0">
                  <StatusBadge status={entry.status} error={entry.error} />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Actions */}
        {fileEntries.length > 0 && (
          <div className="flex flex-wrap gap-3">
            {!allDone && (
              <Button
                onClick={handleUploadAll}
                disabled={isbusy}
                className="gap-2 vault-shadow"
              >
                {isbusy ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <FolderOpen className="h-4 w-4" />
                )}
                {isbusy
                  ? "Uploading…"
                  : `Upload All (${fileEntries.length} file${fileEntries.length !== 1 ? "s" : ""})`}
              </Button>
            )}
            <Button
              variant="ghost"
              onClick={handleClear}
              disabled={isbusy}
              className="gap-1.5 text-muted-foreground"
            >
              <X className="h-4 w-4" />
              Clear
            </Button>
          </div>
        )}

        {/* Empty state */}
        {fileEntries.length === 0 && (
          <p className="text-sm text-muted-foreground">
            Select a folder to see the PDFs it contains.
          </p>
        )}
      </div>

      {/* Reminder */}
      <p className="text-xs text-muted-foreground">
        After uploading, delete the files from your phone folder manually — the
        browser cannot delete files from your device.
      </p>
    </div>
  );
};

export default FolderSync;
