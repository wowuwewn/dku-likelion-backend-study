package com.test.SpringBootApi.service;

import com.test.SpringBootApi.domain.Book;

import java.util.Optional;

public interface BookService {

    public Book save(Book book);
    public Optional<Book> findById(Long id);
    public Book update(Long id, Book book);
    public void delete(Long id);

}
