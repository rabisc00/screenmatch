package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsomeApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private List<Serie> series = new ArrayList<>();
    private Optional<Serie> serieBusca;

    private Scanner leitor = new Scanner(System.in);
    private ConsomeApi consomeApi = new ConsomeApi();
    private ConverteDados conversor = new ConverteDados();
    private SerieRepository repositorioSerie;

    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=" + System.getenv("OMDB_KEY");

    public Principal(SerieRepository repositorioSerie) {
        this.repositorioSerie = repositorioSerie;
    }

    public void exibeMenu() {
        var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar séries buscadas
                4 - Buscar série por título
                5 - Buscar série por ator
                6 - Top 5 séries
                7 - Buscar séries por categoria
                8 - Buscar séries por total de temporadas
                9 - Buscar episódio por trecho
                10 - Top 5 episódios por série
                11 - Buscar episódios a partir de uma data
                
                0 - Sair
                """;
        var opcao = -1;

        while (opcao != 0) {
            System.out.print(menu);

            opcao = leitor.nextInt();
            leitor.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    buscarSeriesPorTotalTemporadas();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosAPartirDeAno();
                    break;
                case 0:
                    System.out.println("\nSaindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }

    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();

        Serie serie = new Serie(dados);
        repositorioSerie.save(serie);

        System.out.println("\n" + dados + "\n");
    }

    private DadosSerie getDadosSerie() {
        System.out.println("\nDigite o nome da série para busca");
        var nomeSerie = leitor.nextLine();
        var json = consomeApi.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie() {
        listarSeriesBuscadas();

        System.out.println("\nVocê quer o episódio de qual das séries?");
        var nomeSerie = leitor.nextLine();

        Optional<Serie> serie = repositorioSerie.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isEmpty()) {
            System.out.println("Série não encontrada");
            return;
        }

        var serieEncontrada = serie.get();
        List<DadosTemporada> temporadas = new ArrayList<>();

        for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
            var url = ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY;
            var json = consomeApi.obterDados(url);

            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
            temporadas.add(dadosTemporada);
        }

        temporadas.forEach(System.out::println);

        List<Episodio> episodios = temporadas.stream()
                .flatMap(d -> d.episodios().stream()
                        .map(e -> new Episodio(d.numero(), e)))
                .collect(Collectors.toList());
        serieEncontrada.setEpisodios(episodios);

        repositorioSerie.save(serieEncontrada);
    }

    private void listarSeriesBuscadas() {
        series = repositorioSerie.findAll();

        System.out.println();
        series.stream().sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
        System.out.println();
    }

    private void buscarSeriePorTitulo() {
        System.out.println("\nDigite o nome da série para busca");
        var nomeSerie = leitor.nextLine();

        serieBusca = repositorioSerie.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()) {
            System.out.println(serieBusca.get());
        } else {
            System.out.println("Série não encontrada!");
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.println("\nQual o nome do ator para buscar as séries?");
        var nomeAtor = leitor.nextLine();

        System.out.println("Avaliações a partir de que valor?");
        var avaliacao = leitor.nextDouble();

        List<Serie> seriesEncontradas = repositorioSerie.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);

        if (!seriesEncontradas.isEmpty()) {
            System.out.println("Séries encontradas com " + nomeAtor + ":");
            seriesEncontradas.forEach(s -> System.out.println(s.getTitulo() + " — Avaliação: " + s.getAvaliacao()));
        }
        else {
            System.out.println("Nenhuma série com " + nomeAtor + " encontrada.");
        }
    }

    private void buscarSeriesPorTotalTemporadas() {
        System.out.println("\nNo máximo quantas temporadas a série pode ter?");
        var totalTemporadas = leitor.nextInt();

        System.out.println("Avaliações a partir de que valor?");
        var avaliacao = leitor.nextDouble();

        List<Serie> seriesEncontradas = repositorioSerie.buscaPorTemporadaEAvaliacao(totalTemporadas, avaliacao);

        System.out.println("Séries encontradas com no máximo " + totalTemporadas + " temporadas:");
        seriesEncontradas.forEach(s ->
                System.out.println(s.getTitulo() + ", " + s.getTotalTemporadas() + " temporadas — avaliação: " + s.getAvaliacao())
        );
    }

    private void buscarTop5Series() {
        List<Serie> seriesTop = repositorioSerie.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach(s -> System.out.println(s.getTitulo() + " — Avaliação: " + s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("\nDe que gênero você quer buscar as séreis?");
        var nomeGenero = leitor.nextLine();

        Categoria categoria = Categoria.fromStringPtBr(nomeGenero);
        List<Serie> seriesPorCategoria = repositorioSerie.findByGenero(categoria);

        System.out.println("Séries de " + nomeGenero + ":");
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Qual o nome do episódio para busca?");
        var trechoEpisodio = leitor.nextLine();

        List<Episodio> episodiosEncontrados = repositorioSerie.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie().getTitulo(), e.getNumeroTemporada(),
                        e.getNumeroEpisodio(), e.getTitulo()));
    }

    private void topEpisodiosPorSerie() {
        buscarSeriePorTitulo();

        if (serieBusca.isPresent()) {
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repositorioSerie.topEpisodiosPorSerie(serie);

            topEpisodios.forEach(e ->
                    System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                            e.getSerie().getTitulo(), e.getNumeroTemporada(),
                            e.getNumeroEpisodio(), e.getTitulo()));
        }
    }

    private void buscarEpisodiosAPartirDeAno() {
        buscarSeriePorTitulo();

        if (serieBusca.isPresent()) {
            Serie serie = serieBusca.get();

            System.out.println("Você quer episódios lançados a partir de que ano?");
            var anoMinimo = leitor.nextInt();
            leitor.nextLine();

            List<Episodio> episodiosAPartirDeAno = repositorioSerie.episodiosAPartirDeAno(serie, anoMinimo);
            episodiosAPartirDeAno.forEach(System.out::println);
        }
    }
}
