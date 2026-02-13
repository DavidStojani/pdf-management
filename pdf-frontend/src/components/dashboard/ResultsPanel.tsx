import { Eye, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DocumentItem } from "@/lib/api";
import FavouriteButton from "./FavouriteButton";

interface Props {
  results: DocumentItem[] | null;
  onClose: () => void;
  onPreview: (doc: DocumentItem) => void;
  onFavouriteToggle: (id: string, isFav: boolean) => void;
}

const ResultsPanel = ({ results, onClose, onPreview, onFavouriteToggle }: Props) => {
  if (results === null) {
    return null;
  }

  return (
    <div className="glass-card vault-shadow-md animate-fade-in overflow-hidden">
      <div className="border-b border-border px-4 py-3">
        <h3 className="text-sm font-semibold text-foreground">
          Search Results ({results.length})
        </h3>
      </div>

      {results.length === 0 ? (
        <div className="py-10 text-center text-sm text-muted-foreground">
          No documents found.
        </div>
      ) : (
        <ul className="divide-y divide-border">
          {results.map((doc) => (
            <li
              key={doc.id}
              className="flex items-center justify-between gap-3 px-4 py-3 transition-colors hover:bg-muted/50"
            >
              <span className="min-w-0 flex-1 truncate text-sm font-medium text-foreground">
                {doc.title}
                {doc.pageCount != null && (
                  <span className="ml-1.5 text-muted-foreground font-normal">({doc.pageCount})</span>
                )}
              </span>
              <div className="flex items-center gap-1">
                <FavouriteButton
                  docId={doc.id}
                  isFavourite={doc.isFavourite ?? false}
                  onToggle={onFavouriteToggle}
                />
                <Button variant="ghost" size="sm" onClick={() => onPreview(doc)} className="gap-1.5 text-primary">
                  <Eye className="h-4 w-4" />
                  <span className="hidden sm:inline">Preview</span>
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <div className="flex justify-end border-t border-border px-4 py-2">
        <Button variant="ghost" size="sm" onClick={onClose} className="gap-1 text-muted-foreground">
          Close <X className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  );
};

export default ResultsPanel;
