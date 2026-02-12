import { useState, useCallback } from "react";
import { Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import UploadModal from "@/components/dashboard/UploadModal";
import SearchBar from "@/components/dashboard/SearchBar";
import ResultsPanel from "@/components/dashboard/ResultsPanel";
import DocumentTabs from "@/components/dashboard/DocumentTabs";
import PdfViewerModal from "@/components/dashboard/PdfViewerModal";
import { DocumentItem } from "@/lib/api";

const Dashboard = () => {
  const [uploadOpen, setUploadOpen] = useState(false);
  const [searchResults, setSearchResults] = useState<DocumentItem[] | null>(null);
  const [previewDoc, setPreviewDoc] = useState<DocumentItem | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleSearchResults = useCallback((results: DocumentItem[]) => {
    setSearchResults(results);
  }, []);

  const handlePreview = useCallback((doc: DocumentItem) => {
    setPreviewDoc(doc);
  }, []);

  const handleUploadSuccess = useCallback(() => {
    setRefreshKey((k) => k + 1);
  }, []);

  const handleFavouriteToggle = useCallback((id: string, isFav: boolean) => {
    setSearchResults((prev) =>
      prev?.map((d) => (d.id === id ? { ...d, isFavourite: isFav } : d)) ?? null
    );
  }, []);

  return (
    <div className="animate-fade-in space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-3xl font-bold text-foreground">Your Documents</h1>
        <Button
          size="lg"
          className="gap-2 vault-shadow"
          onClick={() => setUploadOpen(true)}
        >
          <Upload className="h-5 w-5" />
          Upload
        </Button>
      </div>

      <SearchBar onResults={handleSearchResults} />

      <ResultsPanel
        results={searchResults}
        onClose={() => setSearchResults(null)}
        onPreview={handlePreview}
        onFavouriteToggle={handleFavouriteToggle}
      />

      <DocumentTabs
        key={refreshKey}
        onPreview={handlePreview}
        onFavouriteToggle={handleFavouriteToggle}
      />

      <UploadModal
        open={uploadOpen}
        onOpenChange={setUploadOpen}
        onSuccess={handleUploadSuccess}
      />

      <PdfViewerModal
        document={previewDoc}
        onClose={() => setPreviewDoc(null)}
        onFavouriteToggle={handleFavouriteToggle}
      />
    </div>
  );
};

export default Dashboard;
