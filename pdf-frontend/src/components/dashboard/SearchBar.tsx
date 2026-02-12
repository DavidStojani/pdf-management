import { useState } from "react";
import { Search, Loader2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { api, DocumentItem, getErrorMessage } from "@/lib/api";
import { toast } from "sonner";

interface Props {
  onResults: (results: DocumentItem[]) => void;
}

const SearchBar = ({ onResults }: Props) => {
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const results = await api.searchDocuments(query.trim());
      onResults(results);
    } catch (err: any) {
      toast.error(getErrorMessage(err, "Search failed"));
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") handleSearch();
  };

  return (
    <div className="flex gap-2">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search by tag, keyword, or description…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          className="pl-10 h-12 text-base"
        />
      </div>
      <Button size="lg" onClick={handleSearch} disabled={loading} className="h-12 px-6">
        {loading ? <Loader2 className="h-4 w-4 animate-spin-slow" /> : "Search"}
      </Button>
    </div>
  );
};

export default SearchBar;
