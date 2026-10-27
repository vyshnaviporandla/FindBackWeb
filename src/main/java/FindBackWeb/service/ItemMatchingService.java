
package FindBackWeb.service;

import FindBackWeb.entity.Item;
import FindBackWeb.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ItemMatchingService {

    private final ItemRepository itemRepository;

    public ItemMatchingService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<MatchResult> findMatches(Long itemId) {
        Item target = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Item not found: " + itemId));

        if (!"OPEN".equalsIgnoreCase(target.getStatus())) {
            return List.of();
        }

        String oppositeType =
                "LOST".equalsIgnoreCase(target.getReportType())
                        ? "FOUND" : "LOST";

        List<Item> candidates =
                itemRepository.findByReportTypeIgnoreCase(oppositeType);

        List<MatchResult> matches = new ArrayList<>();

        for (Item candidate : candidates) {
            if (candidate.getId().equals(target.getId())
                    || !"OPEN".equalsIgnoreCase(candidate.getStatus())) {
                continue;
            }

            int score = calculateScore(target, candidate);

            if (score >= 25) {
                matches.add(new MatchResult(candidate, score));
            }
        }

        matches.sort((a, b) -> Integer.compare(b.score(), a.score()));

        return matches;
    }

    private int calculateScore(Item first, Item second) {
        int score = 0;

        if (sameText(first.getCategory(), second.getCategory())) {
            score += 35;
        }

        if (sameText(first.getItemName(), second.getItemName())) {
            score += 40;
        } else if (containsSimilarWords(
                first.getItemName(), second.getItemName())) {
            score += 20;
        }

        if (sameText(first.getLocation(), second.getLocation())) {
            score += 25;
        } else if (containsSimilarWords(
                first.getLocation(), second.getLocation())) {
            score += 10;
        }

        if (containsSimilarWords(
                first.getDescription(), second.getDescription())) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private boolean sameText(String first, String second) {
        return first != null && second != null
                && first.trim().equalsIgnoreCase(second.trim());
    }

    private boolean containsSimilarWords(String first, String second) {
        if (first == null || second == null) {
            return false;
        }

        Set<String> firstWords = words(first);
        Set<String> secondWords = words(second);

        firstWords.retainAll(secondWords);

        return !firstWords.isEmpty();
    }

    private Set<String> words(String text) {
        return java.util.Arrays.stream(
                        text.toLowerCase(Locale.ROOT)
                                .split("[^a-z0-9]+"))
                .filter(word -> word.length() > 2)
                .collect(Collectors.toSet());
    }

    public record MatchResult(Item item, int score) {
    }
}
