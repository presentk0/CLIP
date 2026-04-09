package com.clip.server.word.service;

import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordService {

    private final VideoRepository videoRepository;
    private final CollectedWordRepository collectedWordRepository;

    


}
