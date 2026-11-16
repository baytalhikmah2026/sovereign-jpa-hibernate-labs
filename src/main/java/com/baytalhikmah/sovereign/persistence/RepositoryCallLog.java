package com.baytalhikmah.sovereign.persistence;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/** Remembers every repository call ("CustomerRepository.findByEmail") and how long it took. */
@Component
public class RepositoryCallLog {

    public record Call(String name, Duration duration) {
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();

    public void record(String name, Duration duration) {
        calls.add(new Call(name, duration));
    }

    public int size() {
        return calls.size();
    }

    /** The names of the calls recorded after the first {@code mark} calls. */
    public List<String> namesSince(int mark) {
        return calls.subList(mark, calls.size()).stream().map(Call::name).toList();
    }

}
