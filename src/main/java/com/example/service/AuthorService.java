package com.example.service;

import com.example.domain.Author;

import java.util.List;

/**
 * Created by huanglei on 17/3/28.
 */
public interface AuthorService {

    int add(Author author);
    int update(Author author);
    int delete(Long id);
    Author findAuthor(Long id);
    List<Author> findAuthorList();

}
