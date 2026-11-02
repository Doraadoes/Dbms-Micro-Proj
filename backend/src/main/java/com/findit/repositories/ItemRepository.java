package com.findit.repositories;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.database.InMemoryDatabaseProvider;
import com.findit.models.Item;
import java.util.*;

public class ItemRepository {
    private final DatabaseManager db = DatabaseFactory.getInstance();

    public Optional<Item> findById(Long id) {
        if (id == null) return Optional.empty();

        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("ITEMS").get(id);
            return r != null ? Optional.of(mapItem(r)) : Optional.empty();
        } else {
            String sql = "SELECT * FROM ITEMS WHERE item_id = ?";
            List<Map<String, Object>> rows = db.query(sql, id);
            return rows.isEmpty() ? Optional.empty() : Optional.of(mapItem(rows.get(0)));
        }
    }

    public Item save(Item item) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            if (item.getItemId() == null) {
                item.setItemId(inMem.nextId("SEQ_ITEMS"));
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("item_id", item.getItemId());
            r.put("item_name", item.getItemName());
            r.put("category", item.getCategory());
            r.put("description", item.getDescription());
            r.put("color", item.getColor());
            r.put("image_url", item.getImageUrl());
            inMem.getTables().get("ITEMS").put(item.getItemId(), r);
            return item;
        } else {
            if (item.getItemId() == null) {
                String sql = "INSERT INTO ITEMS (item_id, item_name, category, description, color, image_url) " +
                             "VALUES (SEQ_ITEMS.NEXTVAL, ?, ?, ?, ?, ?)";
                long id = db.executeInsert(sql, item.getItemName(), item.getCategory(), item.getDescription(), item.getColor(), item.getImageUrl());
                item.setItemId(id);
            } else {
                String sql = "UPDATE ITEMS SET item_name = ?, category = ?, description = ?, color = ?, image_url = ? WHERE item_id = ?";
                db.executeUpdate(sql, item.getItemName(), item.getCategory(), item.getDescription(), item.getColor(), item.getImageUrl(), item.getItemId());
            }
            return item;
        }
    }

    public List<Item> search(String keyword, String category, String color) {
        List<Item> list = new ArrayList<>();
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("ITEMS").values()) {
                Item item = mapItem(r);
                boolean matches = true;
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String kw = keyword.trim().toLowerCase();
                    boolean nameMatch = item.getItemName() != null && item.getItemName().toLowerCase().contains(kw);
                    boolean descMatch = item.getDescription() != null && item.getDescription().toLowerCase().contains(kw);
                    if (!nameMatch && !descMatch) matches = false;
                }
                if (matches && category != null && !category.trim().isEmpty()) {
                    if (!category.equalsIgnoreCase(item.getCategory())) matches = false;
                }
                if (matches && color != null && !color.trim().isEmpty()) {
                    if (item.getColor() == null || !item.getColor().equalsIgnoreCase(color.trim())) matches = false;
                }
                if (matches) list.add(item);
            }
        } else {
            StringBuilder sql = new StringBuilder("SELECT * FROM ITEMS WHERE 1=1 ");
            List<Object> params = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append("AND (LOWER(item_name) LIKE ? OR LOWER(description) LIKE ?) ");
                params.add("%" + keyword.trim().toLowerCase() + "%");
                params.add("%" + keyword.trim().toLowerCase() + "%");
            }
            if (category != null && !category.trim().isEmpty()) {
                sql.append("AND category = ? ");
                params.add(category.trim());
            }
            if (color != null && !color.trim().isEmpty()) {
                sql.append("AND LOWER(color) = ? ");
                params.add(color.trim().toLowerCase());
            }
            List<Map<String, Object>> rows = db.query(sql.toString(), params.toArray());
            for (Map<String, Object> r : rows) {
                list.add(mapItem(r));
            }
        }
        return list;
    }

    private Item mapItem(Map<String, Object> r) {
        Item item = new Item();
        item.setItemId(getLong(r, "item_id"));
        item.setItemName(getString(r, "item_name"));
        item.setCategory(getString(r, "category"));
        item.setDescription(getString(r, "description"));
        item.setColor(getString(r, "color"));
        item.setImageUrl(getString(r, "image_url"));
        return item;
    }

    private Long getLong(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(o.toString());
    }

    private String getString(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        return o != null ? o.toString() : null;
    }
}

