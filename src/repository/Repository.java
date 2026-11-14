package repository;

import exception.VertexException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Generic contract demonstrating Java Generics, type safety, and reusable abstraction.
 * Applies uniformly across domain entities (Customer, Account, Transaction).
 *
 * @param <T> Domain entity type
 */
public interface Repository<T> {
    void save(T entity) throws VertexException;
    Optional<T> findById(String id);
    List<T> findAll();
    void update(T entity) throws VertexException;
    boolean delete(String id) throws VertexException;
    List<T> filter(Predicate<? super T> predicate);
    long count();
}
