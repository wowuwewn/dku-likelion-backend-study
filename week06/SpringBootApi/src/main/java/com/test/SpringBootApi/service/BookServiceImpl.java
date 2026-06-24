package com.test.SpringBootApi.service;

import com.test.SpringBootApi.domain.Book;
import com.test.SpringBootApi.respository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Override
    public Book save(Book book) {
        try {
            return bookRepository.save(
                    new Book(
                            book.getTitle(),
                            book.getPrice(),
                            book.getAuthor()
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Optional<Book> findById(Long id) {
        try {
            Optional<Book> bookData = bookRepository.findById(id);
            if (bookData.isPresent()) {
                return bookData;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Book update(Long id, Book book) {
        try {
            Optional<Book> bookData = bookRepository.findById(id);
            if (bookData.isPresent()) {
                Book _book = bookData.get();
                _book.setTitle(book.getTitle());
                _book.setPrice(book.getPrice());
                _book.setAuthor(book.getAuthor());
                bookRepository.save(_book);
                return _book;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        try {
            bookRepository.deleteById(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
