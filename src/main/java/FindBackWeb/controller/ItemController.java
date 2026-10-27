package FindBackWeb.controller;

import FindBackWeb.entity.Item;
import FindBackWeb.repository.AppUserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import org.springframework.security.core.Authentication;

import FindBackWeb.service.ItemMatchingService;

import FindBackWeb.service.ItemMatchingService.MatchResult;

import FindBackWeb.service.ItemService;

import FindBackWeb.service.PhotoStorageService;

import FindBackWeb.service.NotificationService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDate;

import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/items")

@CrossOrigin(origins = "http://localhost:5500")

public class ItemController {

    
private final ItemService itemService;
    
private final ItemMatchingService itemMatchingService;
    
private final PhotoStorageService photoStorageService;
    
private final AppUserRepository appUserRepository;


private final NotificationService notificationService;    
public ItemController(
        ItemService itemService,
        ItemMatchingService itemMatchingService,
        PhotoStorageService photoStorageService,
        AppUserRepository appUserRepository,
        NotificationService notificationService) {
    this.itemService = itemService;
    this.itemMatchingService = itemMatchingService;
    this.photoStorageService = photoStorageService;
    this.appUserRepository = appUserRepository;
    this.notificationService = notificationService;
}
/** Keeps the JSON creation API available for existing clients. */
    
@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    
public Item createItemJson(@Valid @RequestBody Item item, Authentication authentication) {
        item.setOwnerEmail(requireAuthenticatedEmail(authentication));
       Item saved = itemService.createItem(item);
notifyPossibleMatches(saved);
return markOwnership(saved, authentication);
}

    /** Creates a report and optionally stores its photo. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Item createItemWithPhoto(
            @RequestParam String reportType,
            
@RequestParam String itemName,
            
@RequestParam String category,
            
@RequestParam String location,
            
@RequestParam(required = false) String itemDate,
            
@RequestParam(required = false) String description,
            
@RequestParam(required = false) MultipartFile photo,
            
Authentication authentication) {

        
validateReportFields(reportType, itemName, category, location, description);
        
Item item = new Item();
        
applyReportFields(item, reportType, itemName, category, location, itemDate, description);

        item.setOwnerEmail(requireAuthenticatedEmail(authentication));
        
String savedPhotoPath = photoStorageService.store(photo);
        
if (savedPhotoPath != null) item.setImagePath(savedPhotoPath);
        
Item saved = itemService.createItem(item);

notifyPossibleMatches(saved);        
return markOwnership(saved, authentication);
    
}

    
/** Updates an existing report. An omitted photo keeps the current photo unless removePhoto=true. */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Item updateItemWithPhoto(
            @PathVariable Long id,
            @RequestParam String reportType,
            @RequestParam String itemName,
            @RequestParam String category,
            @RequestParam String location,
            @RequestParam(required = false) String itemDate,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile photo,
            @RequestParam(defaultValue = "false") boolean removePhoto,
            Authentication authentication) {

        validateReportFields(reportType, itemName, category, location, description);
        Item item = itemService.getItemById(id);
        requireOwner(item, authentication);
        String oldPhotoPath = item.getImagePath();
        applyReportFields(item, reportType, itemName, category, location, itemDate, description);

        String newPhotoPath = photoStorageService.store(photo);
        if (newPhotoPath != null) {
            item.setImagePath(newPhotoPath);
        } else if (removePhoto) {
            item.setImagePath(null);
        }

        Item savedItem = itemService.saveExistingItem(item);
        if (oldPhotoPath != null && !oldPhotoPath.equals(savedItem.getImagePath())) {
            photoStorageService.delete(oldPhotoPath);
        }
        return markOwnership(savedItem, authentication);
    }

    @GetMapping
    public List<Item> getAllItems(Authentication authentication) {
        return markOwnership(itemService.getAllItems(), authentication);
    }

    @GetMapping("/open")
    public List<Item> getOpenItems(Authentication authentication) {
        return markOwnership(itemService.getOpenItems(), authentication);
    }

    @GetMapping("/{id}")
    public Item getItemById(@PathVariable Long id, Authentication authentication) {
        return markOwnership(itemService.getItemById(id), authentication);
    }

    @GetMapping("/type/{reportType}")
    public List<Item> getItemsByType(@PathVariable String reportType, Authentication authentication) {
        return markOwnership(itemService.getItemsByType(reportType), authentication);
    }

    @GetMapping("/search/name")
    public List<Item> searchByName(@RequestParam String name, Authentication authentication) {
        return markOwnership(itemService.searchByName(name), authentication);
    }

    @GetMapping("/search/location")
    public List<Item> searchByLocation(@RequestParam String location, Authentication authentication) {
        return markOwnership(itemService.searchByLocation(location), authentication);
    }

    @GetMapping("/{id}/matches")
    public List<MatchResult> findMatches(@PathVariable Long id) {
        return itemMatchingService.findMatches(id);
    }

    @PatchMapping("/{id}/status")
    public Item updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Item item = itemService.getItemById(id);
        requireOwner(item, authentication);
        return markOwnership(itemService.updateItemStatus(id, request.get("status")), authentication);
    }

    private void validateReportFields(String reportType, String itemName,
                                      String category, String location, String description) {
        if (reportType == null || !(reportType.equalsIgnoreCase("LOST") || reportType.equalsIgnoreCase("FOUND"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report type must be LOST or FOUND");
        }
        requireText("Item name", itemName, 100);
        requireText("Category", category, 60);
        requireText("Location", location, 200);
        if (description != null && description.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Description must be 1000 characters or fewer");
        }
    }

    private void applyReportFields(Item item, String reportType, String itemName,
                                   String category, String location, String itemDate, String description) {
        item.setReportType(reportType.trim().toUpperCase());
        item.setItemName(itemName.trim());
        item.setCategory(category.trim());
        item.setLocation(location.trim());
        item.setDescription(description == null || description.isBlank() ? null : description.trim());
        if (itemDate == null || itemDate.isBlank()) {
            item.setItemDate(null);
        } else {
            try {
                item.setItemDate(LocalDate.parse(itemDate));
            } catch (DateTimeParseException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Date must be in YYYY-MM-DD format");
            }
        }
    }

    private void requireText(String fieldName, String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        if (value.trim().length() > maximumLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be " + maximumLength + " characters or fewer");
        }
    }


    private String requireAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in to manage reports");
        }
        return appUserRepository.findByEmailIgnoreCase(authentication.getName())
                .map(user -> user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
    }

    private void requireOwner(Item item, Authentication authentication) {
        String email = requireAuthenticatedEmail(authentication);
        if (item.getOwnerEmail() == null || !item.getOwnerEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can edit or resolve only reports submitted by your account");
        }
    }

    private Item markOwnership(Item item, Authentication authentication) {
        String email = null;
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            email = authentication.getName();
        }
        item.setOwnedByCurrentUser(email != null && item.getOwnerEmail() != null
                && item.getOwnerEmail().equalsIgnoreCase(email));
        return item;
    }

        
private List<Item> markOwnership(List<Item> items, Authentication authentication) {
        for (Item item : items) {
            markOwnership(item, authentication);
        }
        return items;
    }
private void notifyPossibleMatches(Item savedItem) {
    try {
        List<MatchResult> matches =
                itemMatchingService.findMatches(savedItem.getId());

        for (MatchResult match : matches) {
            Item matchedItem = match.item();

            String matchedOwnerEmail = matchedItem.getOwnerEmail();
            String newOwnerEmail = savedItem.getOwnerEmail();

            // Do not notify the same person about their own report.
            if (matchedOwnerEmail == null
                    || matchedOwnerEmail.isBlank()
                    || matchedOwnerEmail.equalsIgnoreCase(newOwnerEmail)) {
                continue;
            }

            appUserRepository.findByEmailIgnoreCase(matchedOwnerEmail)
                    .ifPresent(user -> notificationService.createNotification(
                            user.getId(),
                            "A possible match was found for your "
                                    + matchedItem.getReportType().toLowerCase()
                                    + " report: " + matchedItem.getItemName() + ".",
                            savedItem.getId()
                    ));

            // Notify the person who submitted the new report as well.
            appUserRepository.findByEmailIgnoreCase(newOwnerEmail)
                    .ifPresent(user -> notificationService.createNotification(
                            user.getId(),
                            "Your report \"" + savedItem.getItemName()
                                    + "\" may match another report: "
                                    + matchedItem.getItemName() + ".",
                            matchedItem.getId()
                    ));
        }
    } catch (RuntimeException exception) {
        // A notification failure should not undo a successfully saved report.
        System.err.println(
                "Could not create possible-match notifications: "
                        + exception.getMessage());
    }
}
}