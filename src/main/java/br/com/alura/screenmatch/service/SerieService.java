package br.com.alura.screenmatch.service;

import br.com.alura.screenmatch.dto.SerieDTO;
import br.com.alura.screenmatch.model.Serie;
import br.com.alura.screenmatch.repository.SerieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SerieService {
    @Autowired
    private SerieRepository serieRepository;

    public List<SerieDTO> obterTodasSeries() {
        return converteSerie(serieRepository.findAll());
    }

    public List<SerieDTO> obterTop5Series() {
        return converteSerie(serieRepository.findTop5ByOrderByAvaliacaoDesc());
    }

    public List<SerieDTO> obterLancamentos() {
        return converteSerie(serieRepository.top5LancamentosMaisRecentes());
    }

    public SerieDTO obterSeriePorId(Long id) {
        Optional<Serie> serieEncontrada = serieRepository.findById(id);

        if (serieEncontrada.isPresent()) {
            Serie s = serieEncontrada.get();
            return new SerieDTO(s.getId(), s.getTitulo(), s.getGenero(), s.getTotalTemporadas(), s.getAvaliacao(), s.getAtores(), s.getSinopse(), s.getPoster());
        }

        return null;
    }

    private List<SerieDTO> converteSerie(List<Serie> series) {
        return series
                .stream()
                .map(s -> new SerieDTO(s.getId(), s.getTitulo(), s.getGenero(), s.getTotalTemporadas(), s.getAvaliacao(), s.getAtores(), s.getSinopse(), s.getPoster()))
                .collect(Collectors.toList());
    }
}
