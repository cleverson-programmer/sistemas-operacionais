/*
 * TRABALHO DE SISTEMAS OPERACIONAIS - PARTE II (THREADS)
 * EXERCÍCIO 2(b) - PROBLEMA DOS LEITORES E ESCRITORES
 *
 * INTEGRANTES DO GRUPO (IMPRIMIR EM TODOS OS PROGRAMAS):
 * - Rafael Lopes
 * - Cleverson Resende
 * - Matheus Barbosa
 * - Gustavo Cicero
 * - Bernado Melgaço
 *
 * OBJETIVO:
 * Simular várias threads que acessam concorrentemente um vetor de inteiros M.
 * Leitores podem ler em paralelo, mas escritores devem escrever com acesso exclusivo.
 * Usar semáforos para evitar condições de corrida e também evitar starvation (fila justa).
 *
 * ENTRADA (DIGITADA NO TECLADO) - PARÂMETROS DA SIMULAÇÃO:
 * 1) Tamanho do vetor M
 * 2) Quantidade de leitores
 * 3) Quantidade de escritores
 * 4) Operações por thread (quantas leituras/escritas cada thread fará)
 * 5) Tempo mínimo da leitura (ms)
 * 6) Tempo máximo da leitura (ms)
 * 7) Tempo mínimo da escrita (ms)
 * 8) Tempo máximo da escrita (ms)
 * 9) Tempo mínimo de pausa entre operações (ms)
 * 10) Tempo máximo de pausa entre operações (ms)
 *
 * ENTRADA (CASOS DE TESTE SUGERIDOS):
 * Caso 1 (pequeno):
 * tamM=5, leitores=2, escritores=1, ops=5, leituraMin=50, leituraMax=150, escritaMin=80, escritaMax=200, pausaMin=10, pausaMax=50
 *
 * Caso 2 (médio):
 * tamM=10, leitores=5, escritores=2, ops=10, leituraMin=50, leituraMax=200, escritaMin=80, escritaMax=250, pausaMin=10, pausaMax=100
 *
 * Caso 3 (estresse):
 * tamM=50, leitores=20, escritores=10, ops=30, leituraMin=10, leituraMax=80, escritaMin=20, escritaMax=120, pausaMin=0, pausaMax=20
 *
 * COMO COMPILAR (NA RAIZ DO PROJETO):
 * javac -d out src/Exercicio2B_LeitoresEscritores.java
 *
 * COMO EXECUTAR (NA RAIZ DO PROJETO):
 * java -cp out Exercicio2B_LeitoresEscritores
 */
import java.util.ArrayList; // Importa ArrayList para armazenar e aguardar todas as threads no final da simulação.
import java.util.List; // Importa List para trabalhar com uma lista de threads de forma genérica.
import java.util.Random; // Importa Random para sortear índices e tempos, simulando concorrência com atrasos variáveis.
import java.util.Scanner; // Importa Scanner para ler os parâmetros digitados no teclado.
import java.util.concurrent.Semaphore; // Importa Semaphore para coordenar leitores e escritores sem busy wait.

public class Exercicio2B_LeitoresEscritores { // Declara a classe principal do exercício, com main para execução direta.

    private static final class MemoriaCompartilhada { // Declara uma classe interna para conter o vetor M e os semáforos de sincronização.

        private final int[] M; // Declara o vetor de inteiros M que será lido e escrito concorrentemente pelas threads.
        private final Random random; // Declara o gerador aleatório usado para escolher índices e tempos, mantendo a simulação variável.

        private final int tempoLeituraMinMs; // Declara o tempo mínimo de leitura para simular a duração de uma operação de leitura.
        private final int tempoLeituraMaxMs; // Declara o tempo máximo de leitura para simular a duração de uma operação de leitura.
        private final int tempoEscritaMinMs; // Declara o tempo mínimo de escrita para simular a duração de uma operação de escrita.
        private final int tempoEscritaMaxMs; // Declara o tempo máximo de escrita para simular a duração de uma operação de escrita.
        private final int tempoPausaMinMs; // Declara o tempo mínimo de pausa para espaçar operações e gerar interleavings diferentes.
        private final int tempoPausaMaxMs; // Declara o tempo máximo de pausa para espaçar operações e gerar interleavings diferentes.

        private final Semaphore turnstile; // Declara um semáforo "catraca" para garantir ordem justa e evitar starvation.
        private final Semaphore roomEmpty; // Declara um semáforo para garantir exclusividade na escrita (só 1 escritor ou vários leitores).
        private final Semaphore mutexLeitores; // Declara um mutex para proteger o contador de leitores ativos, evitando condição de corrida nele.

        private int leitoresAtivos; // Declara o contador de leitores atualmente dentro da "sala" (lendo) para controlar acesso ao roomEmpty.

        private MemoriaCompartilhada(int tamanho, int tempoLeituraMinMs, int tempoLeituraMaxMs, int tempoEscritaMinMs, int tempoEscritaMaxMs, int tempoPausaMinMs, int tempoPausaMaxMs, Random random) { // Define o construtor que inicializa vetor e semáforos.
            this.M = new int[tamanho]; // Cria o vetor M com o tamanho definido pelo usuário.
            this.random = random; // Guarda o Random recebido, para sortear tempos e índices de forma consistente.
            this.tempoLeituraMinMs = tempoLeituraMinMs; // Armazena o mínimo da leitura para sortear um tempo dentro do intervalo.
            this.tempoLeituraMaxMs = tempoLeituraMaxMs; // Armazena o máximo da leitura para sortear um tempo dentro do intervalo.
            this.tempoEscritaMinMs = tempoEscritaMinMs; // Armazena o mínimo da escrita para sortear um tempo dentro do intervalo.
            this.tempoEscritaMaxMs = tempoEscritaMaxMs; // Armazena o máximo da escrita para sortear um tempo dentro do intervalo.
            this.tempoPausaMinMs = tempoPausaMinMs; // Armazena o mínimo da pausa entre operações para simular alternância de threads.
            this.tempoPausaMaxMs = tempoPausaMaxMs; // Armazena o máximo da pausa entre operações para simular alternância de threads.
            this.turnstile = new Semaphore(1, true); // Inicializa a catraca com 1 e justiça ativada para manter uma fila de chegada.
            this.roomEmpty = new Semaphore(1, true); // Inicializa roomEmpty com 1 e justiça ativada para escrituras aguardarem corretamente.
            this.mutexLeitores = new Semaphore(1, true); // Inicializa o mutex do contador de leitores com 1 e justiça ativada por consistência.
            this.leitoresAtivos = 0; // Começa com zero leitores dentro da área crítica (ninguém lendo no início).
            for (int i = 0; i < M.length; i++) { // Percorre cada posição do vetor para inicializar com um valor conhecido.
                M[i] = i; // Define M[i] com o próprio índice para facilitar visualizar mudanças causadas por escritores.
            }
        }

        private int sortearEntre(int min, int max) { // Declara método para sortear um inteiro no intervalo [min, max], inclusive.
            if (max <= min) { // Se o máximo for menor ou igual ao mínimo, não há intervalo; usa o mínimo.
                return min; // Retorna min como valor fixo para evitar erro no nextInt.
            }
            return min + random.nextInt((max - min) + 1); // Retorna um valor aleatório entre min e max (inclusive).
        }

        private void pausarEntreOperacoes(String nomeThread) throws InterruptedException { // Declara método para pausar e imprimir o motivo, simulando tempo entre ações.
            int pausa = sortearEntre(tempoPausaMinMs, tempoPausaMaxMs); // Sorteia um tempo de pausa para variar o escalonamento das threads.
            if (pausa > 0) { // Verifica se a pausa é maior que zero para evitar sleeps desnecessários.
                System.out.println(nomeThread + " aguardará " + pausa + " ms antes da próxima operação para simular concorrência."); // Imprime a pausa para o usuário ver o interleaving.
                Thread.sleep(pausa); // Pausa a thread para simular tempo de processamento/espera fora da região crítica.
            }
        }

        private void entrarComoLeitor(String nomeLeitor) throws InterruptedException { // Declara a lógica de entrada do leitor na região de leitura compartilhada.
            turnstile.acquire(); // Passa pela catraca para respeitar a ordem justa quando há escritores esperando.
            turnstile.release(); // Libera a catraca imediatamente para permitir que outras threads entrem na fila sem bloquear leitores desnecessariamente.
            mutexLeitores.acquire(); // Entra no mutex para alterar o contador leitoresAtivos com segurança (evita race condition no contador).
            leitoresAtivos++; // Incrementa o contador de leitores ativos, indicando que este leitor está entrando para ler.
            if (leitoresAtivos == 1) { // Verifica se este é o primeiro leitor a entrar.
                roomEmpty.acquire(); // Se for o primeiro leitor, ele bloqueia escritores, garantindo que não haja escrita enquanto leitores leem.
                System.out.println(nomeLeitor + " é o primeiro leitor e bloqueou escrita (roomEmpty adquirido)."); // Imprime que a escrita foi bloqueada por haver leitores.
            }
            System.out.println(nomeLeitor + " entrou para ler. Leitores ativos agora: " + leitoresAtivos + "."); // Imprime que o leitor entrou e quantos leitores estão lendo ao mesmo tempo.
            mutexLeitores.release(); // Sai do mutex liberando a alteração do contador para outros leitores/escritores seguirem.
        }

        private void sairComoLeitor(String nomeLeitor) throws InterruptedException { // Declara a lógica de saída do leitor da região de leitura compartilhada.
            mutexLeitores.acquire(); // Entra no mutex para alterar o contador leitoresAtivos com segurança ao sair.
            leitoresAtivos--; // Decrementa o contador de leitores ativos, indicando que este leitor terminou a leitura.
            System.out.println(nomeLeitor + " terminou de ler e está saindo. Leitores ativos agora: " + leitoresAtivos + "."); // Imprime a saída do leitor e o total restante lendo.
            if (leitoresAtivos == 0) { // Verifica se este foi o último leitor a sair.
                roomEmpty.release(); // Se não há mais leitores, libera roomEmpty para permitir que algum escritor escreva com exclusividade.
                System.out.println(nomeLeitor + " era o último leitor e liberou escrita (roomEmpty liberado)."); // Imprime que a escrita pode voltar a acontecer porque não há leitores ativos.
            }
            mutexLeitores.release(); // Sai do mutex liberando o contador para próximas entradas/saídas de leitores.
        }

        private void entrarComoEscritor(String nomeEscritor) throws InterruptedException { // Declara a lógica de entrada do escritor, garantindo exclusividade.
            turnstile.acquire(); // Bloqueia a catraca, impedindo novos leitores de entrarem enquanto este escritor aguarda (evita starvation do escritor).
            System.out.println(nomeEscritor + " fechou a catraca (turnstile) para impedir novos leitores enquanto aguarda exclusividade."); // Imprime que o escritor evitou que novos leitores entrem na frente.
            roomEmpty.acquire(); // Aguarda exclusividade total no recurso: só passa quando não há leitores e nenhum outro escritor escrevendo.
            System.out.println(nomeEscritor + " adquiriu exclusividade (roomEmpty) e vai escrever no vetor M."); // Imprime que o escritor está sozinho na área compartilhada.
        }

        private void sairComoEscritor(String nomeEscritor) { // Declara a lógica de saída do escritor, liberando exclusividade e a catraca.
            roomEmpty.release(); // Libera a exclusividade do recurso, permitindo que leitores ou outro escritor prossigam.
            System.out.println(nomeEscritor + " liberou exclusividade (roomEmpty) após escrever."); // Imprime que a escrita terminou e a exclusividade foi liberada.
            turnstile.release(); // Abre a catraca para permitir que leitores voltem a entrar (ou próximo escritor entre na fila).
            System.out.println(nomeEscritor + " abriu a catraca (turnstile) para permitir novas entradas."); // Imprime que o fluxo volta ao normal após a escrita.
        }

        private int lerIndice(int indice) { // Declara um método de leitura do vetor para centralizar a operação e deixar claro que é apenas leitura.
            return M[indice]; // Retorna o valor do vetor na posição solicitada, que é o dado que será "impresso" como resultado da leitura.
        }

        private int escreverIndice(int indice, int novoValor) { // Declara um método de escrita do vetor para centralizar a alteração e permitir imprimir antes/depois.
            int antigo = M[indice]; // Armazena o valor anterior para imprimir como a escrita mudou o conteúdo.
            M[indice] = novoValor; // Atualiza o vetor na posição escolhida, simulando uma escrita exclusiva no recurso compartilhado.
            return antigo; // Retorna o valor antigo para que a thread possa imprimir a diferença causada pela escrita.
        }

        private String vetorComoTexto() { // Declara método para transformar o vetor em texto e imprimir o estado atual de M.
            StringBuilder sb = new StringBuilder(); // Cria StringBuilder para montar a string de forma eficiente.
            sb.append("["); // Adiciona o caractere inicial de lista para facilitar visualização do vetor.
            for (int i = 0; i < M.length; i++) { // Percorre todas as posições do vetor para montar a representação completa.
                sb.append(M[i]); // Adiciona o valor atual de M[i] na string.
                if (i < M.length - 1) { // Verifica se ainda não é o último elemento para colocar separador.
                    sb.append(", "); // Adiciona vírgula e espaço entre elementos para facilitar leitura do vetor impresso.
                }
            }
            sb.append("]"); // Fecha a lista com o caractere final para completar a representação do vetor.
            return sb.toString(); // Retorna a string final, que será impressa na tela.
        }
    }

    private static final class Leitor extends Thread { // Declara a classe Thread do leitor, responsável por realizar várias leituras.

        private final int id; // Declara o identificador do leitor para diferenciar os logs de cada thread.
        private final int operacoes; // Declara quantas leituras este leitor executará durante a simulação.
        private final MemoriaCompartilhada memoria; // Declara a referência para a memória compartilhada e semáforos usados pela thread.

        private Leitor(int id, int operacoes, MemoriaCompartilhada memoria) { // Declara o construtor do leitor com seus parâmetros principais.
            this.id = id; // Guarda o id do leitor para imprimir nas mensagens.
            this.operacoes = operacoes; // Guarda a quantidade de operações para o loop principal de leituras.
            this.memoria = memoria; // Guarda a referência à memória compartilhada para ler o vetor e sincronizar.
            setName("Leitor-" + id); // Define o nome da thread, facilitando identificar quem está imprimindo.
        }

        @Override
        public void run() { // Implementa a execução do leitor, que lê repetidamente respeitando a sincronização.
            String nome = getName(); // Obtém o nome da thread para usar como prefixo em todas as mensagens impressas.
            try { // Inicia bloco try para capturar InterruptedException de sleeps e semáforos.
                for (int i = 1; i <= operacoes; i++) { // Loop de leituras: cada iteração representa uma operação de leitura.
                    memoria.pausarEntreOperacoes(nome); // Pausa entre operações para permitir que outras threads intercalem execução.
                    memoria.entrarComoLeitor(nome); // Executa protocolo de entrada do leitor, permitindo leituras paralelas e bloqueando escrita se necessário.
                    int indice = memoria.sortearEntre(0, memoria.M.length - 1); // Sorteia um índice do vetor para este leitor ler, gerando variedade de acesso.
                    int tempoLeitura = memoria.sortearEntre(memoria.tempoLeituraMinMs, memoria.tempoLeituraMaxMs); // Sorteia um tempo de leitura para simular duração real da operação.
                    int valor = memoria.lerIndice(indice); // Lê o valor do vetor no índice sorteado, que será o principal conteúdo impresso.
                    System.out.println(nome + " (op " + i + ") está lendo M[" + indice + "] = " + valor + " (tempo de leitura: " + tempoLeitura + " ms)."); // Imprime a ação de leitura e o valor lido.
                    if (tempoLeitura > 0) { // Verifica se o tempo de leitura é positivo para evitar sleep desnecessário.
                        Thread.sleep(tempoLeitura); // Dorme para simular o tempo que a leitura levou, mantendo o leitor dentro da região de leitura.
                    }
                    memoria.sairComoLeitor(nome); // Executa protocolo de saída do leitor, liberando escrita caso seja o último leitor.
                }
            } catch (InterruptedException e) { // Captura interrupção caso a thread seja interrompida durante a simulação.
                System.out.println(nome + " foi interrompido e encerrará."); // Imprime que o leitor foi interrompido e vai terminar.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção para manter boas práticas.
            }
        }
    }

    private static final class Escritor extends Thread { // Declara a classe Thread do escritor, responsável por realizar várias escritas exclusivas.

        private final int id; // Declara o identificador do escritor para diferenciar logs de cada thread.
        private final int operacoes; // Declara quantas escritas este escritor executará durante a simulação.
        private final MemoriaCompartilhada memoria; // Declara a referência para a memória compartilhada e semáforos usados pela thread.

        private Escritor(int id, int operacoes, MemoriaCompartilhada memoria) { // Declara o construtor do escritor com seus parâmetros principais.
            this.id = id; // Guarda o id do escritor para imprimir e identificar a thread.
            this.operacoes = operacoes; // Guarda a quantidade de operações para o loop principal de escritas.
            this.memoria = memoria; // Guarda a referência à memória compartilhada para escrever no vetor e sincronizar.
            setName("Escritor-" + id); // Define o nome da thread para facilitar leitura das mensagens impressas.
        }

        @Override
        public void run() { // Implementa a execução do escritor, que escreve repetidamente com exclusividade.
            String nome = getName(); // Obtém o nome da thread para usar como prefixo em todos os prints.
            try { // Inicia bloco try para capturar InterruptedException de sleeps e semáforos.
                for (int i = 1; i <= operacoes; i++) { // Loop de escritas: cada iteração representa uma operação de escrita.
                    memoria.pausarEntreOperacoes(nome); // Pausa entre operações para variar o escalonamento e criar concorrência realista.
                    memoria.entrarComoEscritor(nome); // Executa protocolo de entrada do escritor para garantir exclusividade no recurso.
                    int indice = memoria.sortearEntre(0, memoria.M.length - 1); // Sorteia um índice do vetor para escrever, simulando acesso aleatório.
                    int novoValor = memoria.sortearEntre(0, 999); // Sorteia um novo valor para gravar, tornando as mudanças visíveis nos logs.
                    int tempoEscrita = memoria.sortearEntre(memoria.tempoEscritaMinMs, memoria.tempoEscritaMaxMs); // Sorteia tempo de escrita para simular duração real da operação.
                    int antigo = memoria.escreverIndice(indice, novoValor); // Escreve no vetor e recebe o valor antigo para imprimir antes/depois.
                    System.out.println(nome + " (op " + i + ") escreveu em M[" + indice + "]: " + antigo + " -> " + novoValor + " (tempo de escrita: " + tempoEscrita + " ms)."); // Imprime a alteração realizada pelo escritor.
                    if (tempoEscrita > 0) { // Verifica se o tempo de escrita é positivo para evitar sleep desnecessário.
                        Thread.sleep(tempoEscrita); // Dorme para simular o tempo gasto escrevendo, mantendo exclusividade durante esse período.
                    }
                    memoria.sairComoEscritor(nome); // Executa protocolo de saída do escritor, liberando exclusividade e reabrindo a catraca.
                }
            } catch (InterruptedException e) { // Captura interrupção caso a thread seja interrompida durante a simulação.
                System.out.println(nome + " foi interrompido e encerrará."); // Imprime que o escritor foi interrompido e vai terminar.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção para manter boas práticas.
            }
        }
    }

    private static int lerInteiro(Scanner scanner, String mensagem) { // Declara um método utilitário para ler um inteiro com validação e mensagem clara.
        System.out.print(mensagem); // Imprime a mensagem para orientar o usuário sobre o que deve ser digitado.
        while (!scanner.hasNextInt()) { // Enquanto a entrada não for um inteiro válido, continua solicitando.
            System.out.println("Valor inválido. Digite um número inteiro."); // Informa o erro e pede correção ao usuário.
            scanner.next(); // Descarta o token inválido para tentar ler novamente na próxima iteração.
            System.out.print(mensagem); // Reimprime a mensagem original para solicitar o mesmo valor.
        }
        return scanner.nextInt(); // Retorna o valor inteiro lido corretamente, que será usado como parâmetro da simulação.
    }

    public static void main(String[] args) throws Exception { // Declara o main que configura a simulação, cria threads e imprime resultados.
        System.out.println("Integrantes do grupo: Rafael Lopes, Cleverson Resende, Matheus Barbosa, Gustavo Cicero, Bernado Melgaço."); // Imprime os nomes do grupo conforme exigido.
        System.out.println("Exercício 2(b) - Problema dos Leitores e Escritores (Threads + Semáforos)."); // Imprime o título do exercício para contextualizar a execução.
        Scanner scanner = new Scanner(System.in); // Cria o Scanner para leitura dos parâmetros pelo teclado.
        int tamanho = lerInteiro(scanner, "Digite o tamanho do vetor M: "); // Lê o tamanho do vetor para criar a memória compartilhada.
        int qtdLeitores = lerInteiro(scanner, "Digite a quantidade de leitores: "); // Lê quantas threads leitoras serão criadas.
        int qtdEscritores = lerInteiro(scanner, "Digite a quantidade de escritores: "); // Lê quantas threads escritoras serão criadas.
        int operacoes = lerInteiro(scanner, "Digite quantas operações por thread (leituras/escritas): "); // Lê quantas operações cada thread executará.
        int leituraMin = lerInteiro(scanner, "Digite o tempo mínimo da leitura (ms): "); // Lê o tempo mínimo para simular leitura.
        int leituraMax = lerInteiro(scanner, "Digite o tempo máximo da leitura (ms): "); // Lê o tempo máximo para simular leitura.
        int escritaMin = lerInteiro(scanner, "Digite o tempo mínimo da escrita (ms): "); // Lê o tempo mínimo para simular escrita.
        int escritaMax = lerInteiro(scanner, "Digite o tempo máximo da escrita (ms): "); // Lê o tempo máximo para simular escrita.
        int pausaMin = lerInteiro(scanner, "Digite o tempo mínimo de pausa entre operações (ms): "); // Lê o tempo mínimo de pausa para alternar execução.
        int pausaMax = lerInteiro(scanner, "Digite o tempo máximo de pausa entre operações (ms): "); // Lê o tempo máximo de pausa para alternar execução.
        Random random = new Random(); // Cria o gerador de aleatoriedade usado em toda a simulação.
        MemoriaCompartilhada memoria = new MemoriaCompartilhada(tamanho, leituraMin, leituraMax, escritaMin, escritaMax, pausaMin, pausaMax, random); // Cria o estado compartilhado com vetor e semáforos.
        System.out.println("Vetor M inicial: " + memoria.vetorComoTexto()); // Imprime o vetor inicial para comparar com o resultado final após escritas concorrentes.
        List<Thread> threads = new ArrayList<>(); // Cria uma lista para armazenar todas as threads e aguardar o término delas com join.
        for (int i = 1; i <= qtdLeitores; i++) { // Loop para criar e iniciar todas as threads leitoras.
            Leitor leitor = new Leitor(i, operacoes, memoria); // Cria um leitor com id, número de operações e referência para a memória compartilhada.
            threads.add(leitor); // Adiciona a thread na lista para controle e join no final.
            leitor.start(); // Inicia a thread leitora para começar as leituras concorrentemente.
        }
        for (int i = 1; i <= qtdEscritores; i++) { // Loop para criar e iniciar todas as threads escritoras.
            Escritor escritor = new Escritor(i, operacoes, memoria); // Cria um escritor com id, número de operações e referência para a memória compartilhada.
            threads.add(escritor); // Adiciona a thread na lista para controle e join no final.
            escritor.start(); // Inicia a thread escritora para começar as escritas com exclusividade garantida.
        }
        for (Thread t : threads) { // Percorre a lista de threads para aguardar o fim de todas elas.
            t.join(); // Espera cada thread terminar para garantir que a simulação finalize antes de imprimir o resultado final.
        }
        System.out.println("Todas as threads finalizaram."); // Imprime que a execução concorrente terminou, então o estado final do vetor já está estável.
        System.out.println("Vetor M final: " + memoria.vetorComoTexto()); // Imprime o vetor final para mostrar os efeitos das escritas feitas pelos escritores.
        scanner.close(); // Fecha o Scanner para liberar o recurso do System.in, seguindo boas práticas.
    }
}

