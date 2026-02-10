import { useState } from "react";
import { Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import { toast } from "sonner";

interface Props {
  docId: string;
  isFavourite: boolean;
  onToggle: (id: string, isFav: boolean) => void;
}

const FavouriteButton = ({ docId, isFavourite, onToggle }: Props) => {
  const [loading, setLoading] = useState(false);

  const handleToggle = async () => {
    const newState = !isFavourite;
    // Optimistic update
    onToggle(docId, newState);
    setLoading(true);
    try {
      if (newState) {
        await api.addFavourite(docId);
        toast.success("Added to favourites");
      } else {
        await api.removeFavourite(docId);
        toast.success("Removed from favourites");
      }
    } catch (err: any) {
      // Revert
      onToggle(docId, isFavourite);
      toast.error(err.message || "Failed to update favourite");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Button
      variant="ghost"
      size="icon"
      className="h-8 w-8"
      onClick={handleToggle}
      disabled={loading}
      aria-label={isFavourite ? "Remove from favourites" : "Add to favourites"}
    >
      <Star
        className={`h-4 w-4 transition-colors ${
          isFavourite ? "fill-warning text-warning" : "text-muted-foreground"
        }`}
      />
    </Button>
  );
};

export default FavouriteButton;
