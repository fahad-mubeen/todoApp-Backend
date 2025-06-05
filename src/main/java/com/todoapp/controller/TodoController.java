package com.todoapp.controller;

import com.todoapp.model.Todo;
import com.todoapp.model.User;
import com.todoapp.service.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public ResponseEntity<List<Todo>> getTodos(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<Todo> todos = todoService.getTodosForUser(user);
        return ResponseEntity.ok(todos);
    }

    @PostMapping
    public ResponseEntity<?> createTodo(@RequestBody Map<String, Object> request,
                                        @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String text = (String) request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
        }

        Todo todo = new Todo(text.trim(), user);
        Todo created = todoService.createTodo(todo);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTodo(@PathVariable Long id,
                                        @RequestBody Map<String, Object> request,
                                        @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        return todoService.getTodoById(id)
                .map(todo -> {
                    if (!todo.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
                    }

                    String text = (String) request.get("text");
                    Boolean completed = (Boolean) request.get("completed");

                    if (text != null) {
                        todo.setText(text.trim());
                    }
                    if (completed != null) {
                        todo.setCompleted(completed);
                    }

                    Todo updated = todoService.updateTodo(todo);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        return todoService.getTodoById(id)
                .map(todo -> {
                    if (!todo.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
                    }
                    todoService.deleteTodoById(id);
                    return ResponseEntity.ok(Map.of("message", "Todo deleted successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/healthCheck")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("message", "allGood"));
    }
}