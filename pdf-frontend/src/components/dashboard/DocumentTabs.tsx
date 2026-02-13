import { useState, useEffect } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Eye, FileText, Star, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { api, DocumentItem } from "@/lib/api";
import { toast } from "sonner";
import FavouriteButton from "./FavouriteButton";

interface Props {
  onPreview: (doc: DocumentItem) => void;
  onFavouriteToggle: (id: string, isFav: boolean) => void;
}

const DocumentTabs = ({ onPreview, onFavouriteToggle }: Props) => {
  const [tab, setTab] = useState("all");
  const [allDocs, setAllDocs] = useState<DocumentItem[]>([]);
  const [favDocs, setFavDocs] = useState<DocumentItem[]>([]);
  const [loadingAll, setLoadingAll] = useState(false);
  const [loadingFav, setLoadingFav] = useState(false);

  useEffect(() => {
    const fetchAll = async () => {
      setLoadingAll(true);
      try {
        const docs = await api.searchDocuments("");
        setAllDocs(docs);
      } catch {
        // silently fail - API might not be ready
      } finally {
        setLoadingAll(false);
      }
    };
    fetchAll();
  }, []);

  useEffect(() => {
    if (tab === "favourites") {
      const fetchFav = async () => {
        setLoadingFav(true);
        try {
          const docs = await api.getFavourites();
          setFavDocs(docs);
        } catch {
          // silently fail
        } finally {
          setLoadingFav(false);
        }
      };
      fetchFav();
    }
  }, [tab]);

  const handleFavToggle = (id: string, isFav: boolean) => {
    setAllDocs((prev) => prev.map((d) => (d.id === id ? { ...d, isFavourite: isFav } : d)));
    setFavDocs((prev) =>
      isFav
        ? prev
        : prev.filter((d) => d.id !== id)
    );
    onFavouriteToggle(id, isFav);
  };

  const renderList = (docs: DocumentItem[], loading: boolean) => {
    if (loading) {
      return (
        <div className="flex justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin-slow text-primary" />
        </div>
      );
    }
    if (docs.length === 0) {
      return (
        <div className="flex flex-col items-center gap-2 py-12 text-muted-foreground">
          <FileText className="h-10 w-10" />
          <p className="text-sm">No documents yet</p>
        </div>
      );
    }
    return (
      <ul className="divide-y divide-border">
        {docs.map((doc) => (
          <li
            key={doc.id}
            className="flex items-center justify-between gap-3 px-1 py-3 transition-colors hover:bg-muted/50 rounded-lg"
          >
            <div className="flex min-w-0 flex-1 items-center gap-2">
              <FileText className="h-4 w-4 shrink-0 text-primary" />
              <span className="truncate text-sm font-medium text-foreground">
                {doc.title}
                {doc.pageCount != null && (
                  <span className="ml-1.5 text-muted-foreground font-normal">({doc.pageCount})</span>
                )}
              </span>
            </div>
            <div className="flex items-center gap-1">
              <FavouriteButton docId={doc.id} isFavourite={doc.isFavourite ?? false} onToggle={handleFavToggle} />
              <Button variant="ghost" size="sm" onClick={() => onPreview(doc)} className="gap-1.5 text-primary">
                <Eye className="h-4 w-4" />
                <span className="hidden sm:inline">Preview</span>
              </Button>
            </div>
          </li>
        ))}
      </ul>
    );
  };

  return (
    <Tabs value={tab} onValueChange={setTab} className="w-full">
      <TabsList className="mb-4 w-full max-w-xs">
        <TabsTrigger value="all" className="flex-1 gap-1.5">
          <FileText className="h-3.5 w-3.5" /> All
        </TabsTrigger>
        <TabsTrigger value="favourites" className="flex-1 gap-1.5">
          <Star className="h-3.5 w-3.5" /> Favourites
        </TabsTrigger>
      </TabsList>

      <TabsContent value="all" className="glass-card p-4">
        {renderList(allDocs, loadingAll)}
      </TabsContent>

      <TabsContent value="favourites" className="glass-card p-4">
        {renderList(favDocs, loadingFav)}
      </TabsContent>
    </Tabs>
  );
};

export default DocumentTabs;
