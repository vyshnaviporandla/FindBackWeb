package FindBackWeb.service;

import FindBackWeb.entity.Item;
import FindBackWeb.repository.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Item createItem(Item item) {
        item.setId(null);
        validateReportType(item.getReportType());
        item.setReportType(item.getReportType().toUpperCase());
        item.setStatus("OPEN");
        return itemRepository.save(item);
    }

    /** Saves an existing item after its editable fields have been updated. */
    public Item saveExistingItem(Item item) {
        if (item.getId() == null || !itemRepository.existsById(item.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
        validateReportType(item.getReportType());
        item.setReportType(item.getReportType().toUpperCase());
        return itemRepository.save(item);
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    public List<Item> getOpenItems() {
        return itemRepository.findByStatusIgnoreCase("OPEN");
    }

    public List<Item> getItemsByType(String reportType) {
        validateReportType(reportType);
        return itemRepository.findByReportTypeIgnoreCase(reportType);
    }

    public List<Item> searchByName(String name) {
        return itemRepository.findByItemNameContainingIgnoreCase(name);
    }

    public List<Item> searchByLocation(String location) {
        return itemRepository.findByLocationContainingIgnoreCase(location);
    }

    public Item updateItemStatus(Long id, String status) {
        if (status == null || !(status.equalsIgnoreCase("OPEN")
                || status.equalsIgnoreCase("RESOLVED") || status.equalsIgnoreCase("CLOSED"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status must be OPEN, RESOLVED, or CLOSED");
        }
        Item item = getItemById(id);
        item.setStatus(status.toUpperCase());
        return itemRepository.save(item);
    }

    private void validateReportType(String reportType) {
        if (reportType == null || !(reportType.equalsIgnoreCase("LOST")
                || reportType.equalsIgnoreCase("FOUND"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Report type must be LOST or FOUND");
        }
    }
}
