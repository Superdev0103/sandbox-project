package com.example;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.*;

@ContextServiceDefinition(name = "java:app/concurrent/MyContextService",
        propagated = APPLICATION,
        cleared = {TRANSACTION, SECURITY},
        unchanged = ALL_REMAINING
)
@ManagedExecutorDefinition(name = "java:module/concurrent/MyExecutor",
        context = "java:app/concurrent/MyContextService",
        maxAsync = 5
)
@ApplicationScoped
public class TodoService {

    @Resource(lookup = "java:module/concurrent/MyExecutor")
    ManagedExecutorService executorService;

    @PersistenceContext
    EntityManager entityManager;

    @Asynchronous
    public CompletableFuture<List<Todo>> getAllTodosAsync() {
        Supplier<List<Todo>> todos = () -> entityManager.createQuery("select t from Todo t", Todo.class).getResultList();
        return executorService.supplyAsync(todos);

//        var todos = entityManager.createQuery("select t from Todo t", Todo.class).getResultList();
//        return Asynchronous.Result.complete(todos);
    }

    public List<Todo> getAllTodos() {
        return entityManager.createQuery("select t from Todo t", Todo.class).getResultList();
    }

    @Transactional
    public Todo create(Todo todo) {
        entityManager.persist(todo);
        return todo;
    }

    // when using @Transactional with @Asynchronous, only `REQUIRES_NEW` and `NOT_SUPPORTED` are supported.
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Asynchronous
    public CompletionStage<Todo> createAsync(Todo todo) {
        entityManager.persist(todo);
        return executorService.supplyAsync(() -> todo);
    }
}
