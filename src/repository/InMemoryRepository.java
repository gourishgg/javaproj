package repository;

import exception.DuplicateRecordException;
import exception.VertexException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Thread-safe generic in-memory repository implementation.
 * Demonstrates:
 * - Java Generics & Type Parameters
 * - Higher-order functions (Function<T, String>, Predicate<? super T>)
 * - Bounded wildcards
 * - Stream API integration
 *
 * @param <T> Domain entity type
 */
public class InMemoryRepository<T> implements Repository<T> {
    private final Map<String, T> storage = new ConcurrentHashMap<>();
    private final Function<T, String> keyExtractor;
    private final String entityName;

    public InMemoryRepository(String entityName, Function<T, String> keyExtractor) {
        this.entityName = entityName;
        this.keyExtractor = keyExtractor;
    }

    @Override
    public void save(T entity) throws VertexException {
        if (entity == null) {
            throw new VertexException("Cannot save null " + entityName + " entity.");
        }
        String key = keyExtractor.apply(entity);
        if (key == null || key.trim().isEmpty()) {
            throw new VertexException("Entity key cannot be null or empty.");
        }
        if (storage.containsKey(key)) {
            throw new DuplicateRecordException(entityName + " with ID '" + key + "' already exists.");
        }
        storage.put(key, entity);
    }

    @Override
    public Optional<T> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id.trim().toUpperCase()));
    }

    @Override
    public List<T> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(storage.values()));
    }

    @Override
    public void update(T entity) throws VertexException {
        if (entity == null) {
            throw new VertexException("Cannot update null " + entityName + " entity.");
        }
        String key = keyExtractor.apply(entity);
        if (!storage.containsKey(key)) {
            throw new VertexException(entityName + " with ID '" + key + "' not found for update.");
        }
        storage.put(key, entity);
    }

    @Override
    public boolean delete(String id) throws VertexException {
        if (id == null) return false;
        return storage.remove(id.trim().toUpperCase()) != null;
    }

    @Override
    public List<T> filter(Predicate<? super T> predicate) {
        return storage.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return storage.size();
    }
}
