/*
 * TRABALHO DE SISTEMAS OPERACIONAIS - PARTE II (THREADS)
 * EXERCÍCIO 2(d) - PROBLEMA DOS FILÓSOFOS (JANTAR DOS FILÓSOFOS)
 *
 * INTEGRANTES DO GRUPO:
 * - Rafael Lopes
 * - Cleverson Resende
 * - Matheus Barbosa
 * - Bernado Melgaço
 *
 * ENTRADA - PARÂMETROS DA SIMULAÇÃO:
 * 1) Quantidade de refeições por filósofo
 * 2) Tempo mínimo de meditação (ms)
 * 3) Tempo máximo de meditação (ms)
 * 4) Tempo mínimo de comer (ms)
 * 5) Tempo máximo de comer (ms)
 *
 * ENTRADA CASOS DE TESTE::
 * Caso 1 (pequeno):
 * refeicoes=3, meditarMin=50, meditarMax=150, comerMin=80, comerMax=200
 *
 * Caso 2 (médio):
 * refeicoes=10, meditarMin=50, meditarMax=300, comerMin=80, comerMax=300
 *
 * Caso 3 (dificil):
 * refeicoes=50, meditarMin=10, meditarMax=80, comerMin=10, comerMax=80
 *
 * COMO COMPILAR (NA RAIZ DO PROJETO):
 * javac -d out src/Exercicio2D_Filosofos.java
 *
 * COMO EXECUTAR (NA RAIZ DO PROJETO):
 * java -cp out Exercicio2D_Filosofos
 */
import java.util.Random; // Importa Random para sortear tempos de meditar e comer e assim gerar concorrência variável.
import java.util.Scanner; // Importa Scanner para ler os parâmetros de entrada digitados pelo usuário.
import java.util.concurrent.Semaphore; // Importa Semaphore para sincronização (hashis e garçom) sem busy wait.

public class Exercicio2D_Filosofos { // Declara a classe principal do exercício com o método main.

    private static final int QUANTIDADE_FILOSOFOS = 5; // Define a quantidade fixa de filósofos conforme o enunciado (cinco filósofos).

    private static final class Mesa { // Declara uma classe para agrupar os semáforos (hashis e garçom) e recursos compartilhados.

        private final Semaphore[] hashis; // Declara um array de semáforos, um para cada hashi, garantindo posse exclusiva de cada hashi.
        private final Semaphore garcom; // Declara um semáforo "garçom" para limitar quantos filósofos tentam pegar hashis simultaneamente.
        private final Random random; // Declara o Random para sortear tempos de meditar e comer.

        private final int meditarMinMs; // Declara o tempo mínimo de meditação para sorteio.
        private final int meditarMaxMs; // Declara o tempo máximo de meditação para sorteio.
        private final int comerMinMs; // Declara o tempo mínimo de comer para sorteio.
        private final int comerMaxMs; // Declara o tempo máximo de comer para sorteio.

        private Mesa(int meditarMinMs, int meditarMaxMs, int comerMinMs, int comerMaxMs, Random random) { // Define construtor que inicializa semáforos e parâmetros.
            this.hashis = new Semaphore[QUANTIDADE_FILOSOFOS]; // Cria o array para armazenar os 5 semáforos de hashis.
            for (int i = 0; i < QUANTIDADE_FILOSOFOS; i++) { // Percorre os 5 índices para inicializar cada semáforo do hashi.
                hashis[i] = new Semaphore(1, true); // Inicializa cada hashi com 1 (livre) e justiça true para manter fila de espera.
            }
            this.garcom = new Semaphore(QUANTIDADE_FILOSOFOS - 1, true); // Inicializa o garçom com 4 permissões para evitar deadlock.
            this.meditarMinMs = meditarMinMs; // Armazena o mínimo de meditação para sortear.
            this.meditarMaxMs = meditarMaxMs; // Armazena o máximo de meditação para sortear.
            this.comerMinMs = comerMinMs; // Armazena o mínimo de comer para sortear.
            this.comerMaxMs = comerMaxMs; // Armazena o máximo de comer para sortear.
            this.random = random; // Armazena o Random para sorteios de tempos.
        }

        private int sortearEntre(int min, int max) { // Declara método auxiliar para sortear um inteiro entre min e max, inclusive.
            if (max <= min) { // Se max <= min, não há intervalo real, então usa o mínimo.
                return min; // Retorna min como valor fixo para evitar erro no nextInt.
            }
            return min + random.nextInt((max - min) + 1); // Retorna um valor aleatório no intervalo [min, max].
        }

        private int tempoMeditar() { // Declara um método para obter o tempo de meditação sorteado para um filósofo.
            return sortearEntre(meditarMinMs, meditarMaxMs); // Retorna um tempo aleatório entre mínimo e máximo de meditação.
        }

        private int tempoComer() { // Declara um método para obter o tempo de comer sorteado para um filósofo.
            return sortearEntre(comerMinMs, comerMaxMs); // Retorna um tempo aleatório entre mínimo e máximo de comer.
        }
    }

    private static final class Filosofo extends Thread { // Declara a thread do filósofo, que alterna entre meditar e comer.

        private final int id; // Declara o identificador do filósofo (0 a 4) para localizar hashis e imprimir mensagens.
        private final int refeicoes; // Declara quantas vezes este filósofo deve comer (número de ciclos de "comer").
        private final Mesa mesa; // Declara a referência para a mesa (semáforos e tempos) compartilhada entre todos.

        private Filosofo(int id, int refeicoes, Mesa mesa) { // Construtor que recebe id, quantidade de refeições e referência à mesa.
            this.id = id; // Armazena o id para calcular hashi esquerdo e direito.
            this.refeicoes = refeicoes; // Armazena quantas refeições serão realizadas para terminar em tempo finito.
            this.mesa = mesa; // Armazena referência para semáforos e para sortear tempos.
            setName("Filosofo-" + id); // Define nome da thread para facilitar identificar cada filósofo nos logs.
        }

        private int hashiEsquerdo() { // Declara método para calcular o índice do hashi à esquerda do filósofo.
            return id; // Usa a convenção: hashi esquerdo do filósofo i é o hashi i.
        }

        private int hashiDireito() { // Declara método para calcular o índice do hashi à direita do filósofo.
            return (id + 1) % QUANTIDADE_FILOSOFOS; // Usa (i+1) mod 5 para obter o hashi da direita em mesa circular.
        }

        @Override
        public void run() { // Implementa o comportamento do filósofo ao longo da simulação.
            String nome = getName(); // Obtém o nome da thread para usar como prefixo nas mensagens.
            try { // Inicia bloco try para tratar InterruptedException de semáforos e sleeps.
                for (int r = 1; r <= refeicoes; r++) { // Loop para executar o ciclo "meditar -> comer" a quantidade definida.
                    int tempoMeditar = mesa.tempoMeditar(); // Sorteia por quanto tempo este filósofo vai meditar nesta rodada.
                    System.out.println(nome + " vai meditar por " + tempoMeditar + " ms antes da refeição " + r + "."); // Imprime a meditação e a refeição que virá.
                    if (tempoMeditar > 0) { // Verifica se o tempo é positivo para evitar sleep desnecessário.
                        Thread.sleep(tempoMeditar); // Dorme para simular a meditação, permitindo que outras threads executem.
                    }
                    System.out.println(nome + " está com fome e pediu permissão ao garçom para tentar pegar os hashis."); // Imprime que ele vai entrar na fase crítica de tentar pegar hashis.
                    mesa.garcom.acquire(); // Pede permissão ao garçom para reduzir chance de deadlock limitando concorrência na pega dos hashis.
                    int e = hashiEsquerdo(); // Calcula o índice do hashi esquerdo para imprimir e para adquirir o semáforo correto.
                    int d = hashiDireito(); // Calcula o índice do hashi direito para imprimir e para adquirir o semáforo correto.
                    System.out.println(nome + " recebeu permissão do garçom e vai tentar pegar o hashi esquerdo (" + e + ")."); // Imprime que vai tentar pegar o primeiro hashi.
                    mesa.hashis[e].acquire(); // Adquire o hashi esquerdo, garantindo exclusividade sobre esse recurso.
                    System.out.println(nome + " pegou o hashi esquerdo (" + e + ") e agora vai tentar pegar o hashi direito (" + d + ")."); // Imprime que já pegou um hashi e vai tentar o outro.
                    mesa.hashis[d].acquire(); // Adquire o hashi direito, garantindo que agora tem os dois recursos necessários para comer.
                    System.out.println(nome + " pegou os dois hashis (" + e + " e " + d + ") e vai comer a refeição " + r + "."); // Imprime que ele conseguiu os dois hashis e vai comer.
                    int tempoComer = mesa.tempoComer(); // Sorteia por quanto tempo este filósofo vai comer, simulando duração variável.
                    System.out.println(nome + " está comendo por " + tempoComer + " ms."); // Imprime o tempo de comer para acompanhar a concorrência.
                    if (tempoComer > 0) { // Verifica se o tempo é positivo para evitar sleep desnecessário.
                        Thread.sleep(tempoComer); // Dorme para simular o ato de comer, mantendo os hashis ocupados durante esse período.
                    }
                    System.out.println(nome + " terminou de comer a refeição " + r + " e vai devolver os hashis."); // Imprime que o filósofo acabou e vai liberar os recursos.
                    mesa.hashis[d].release(); // Libera o hashi direito para que o vizinho possa usar em outra oportunidade.
                    System.out.println(nome + " devolveu o hashi direito (" + d + ")."); // Imprime que o hashi direito foi devolvido.
                    mesa.hashis[e].release(); // Libera o hashi esquerdo para que o outro vizinho também possa usar.
                    System.out.println(nome + " devolveu o hashi esquerdo (" + e + ")."); // Imprime que o hashi esquerdo foi devolvido.
                    mesa.garcom.release(); // Devolve a permissão ao garçom para que outro filósofo possa tentar pegar hashis.
                    System.out.println(nome + " liberou o garçom após finalizar a refeição " + r + "."); // Imprime que o garçom foi liberado e o sistema pode prosseguir.
                }
                System.out.println(nome + " terminou todas as " + refeicoes + " refeições e encerrará sua thread."); // Imprime que este filósofo concluiu e vai encerrar.
            } catch (InterruptedException e) { // Captura interrupção se a thread for interrompida durante semáforos/sleep.
                System.out.println(nome + " foi interrompido e encerrará."); // Imprime que a thread foi interrompida.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção como boa prática.
            }
        }
    }

    private static int lerInteiro(Scanner scanner, String mensagem) { // Declara um método utilitário para ler inteiros com validação.
        System.out.print(mensagem); // Imprime a mensagem solicitando a entrada do usuário.
        while (!scanner.hasNextInt()) { // Enquanto o próximo token não for um inteiro, repete o pedido.
            System.out.println("Valor inválido. Digite um número inteiro."); // Informa que o valor não é um inteiro e pede correção.
            scanner.next(); // Descarta o token inválido para tentar novamente.
            System.out.print(mensagem); // Reimprime a mensagem original para solicitar o mesmo valor.
        }
        return scanner.nextInt(); // Retorna o inteiro válido digitado pelo usuário.
    }

    public static void main(String[] args) throws Exception { // Declara o main que lê entradas, cria a mesa e inicia as threads dos filósofos.
        System.out.println("Integrantes do grupo: Rafael Lopes, Cleverson Resende, Matheus Barbosa, Gustavo Cicero, Bernado Melgaço."); // Imprime os integrantes conforme exigido.
        System.out.println("Exercício 2(d) - Jantar dos Filósofos (Threads + Semáforos)."); // Imprime o título para o usuário saber o que está rodando.
        Scanner scanner = new Scanner(System.in); // Cria Scanner para ler entrada do teclado.
        int refeicoes = lerInteiro(scanner, "Digite a quantidade de refeições por filósofo: "); // Lê quantas refeições cada filósofo fará antes de encerrar.
        int meditarMin = lerInteiro(scanner, "Digite o tempo mínimo de meditação (ms): "); // Lê o tempo mínimo de meditação para sorteio.
        int meditarMax = lerInteiro(scanner, "Digite o tempo máximo de meditação (ms): "); // Lê o tempo máximo de meditação para sorteio.
        int comerMin = lerInteiro(scanner, "Digite o tempo mínimo de comer (ms): "); // Lê o tempo mínimo de comer para sorteio.
        int comerMax = lerInteiro(scanner, "Digite o tempo máximo de comer (ms): "); // Lê o tempo máximo de comer para sorteio.
        Random random = new Random(); // Cria um Random para sortear tempos de meditar e comer.
        Mesa mesa = new Mesa(meditarMin, meditarMax, comerMin, comerMax, random); // Cria a mesa com semáforos e parâmetros da simulação.
        Thread[] filosofos = new Thread[QUANTIDADE_FILOSOFOS]; // Cria um array para armazenar as threads dos 5 filósofos.
        for (int i = 0; i < QUANTIDADE_FILOSOFOS; i++) { // Loop para criar cada filósofo com seu id.
            filosofos[i] = new Filosofo(i, refeicoes, mesa); // Cria a thread do filósofo com id, número de refeições e referência à mesa.
            filosofos[i].start(); // Inicia a thread do filósofo para começar a meditar e comer concorrentemente.
        }
        for (int i = 0; i < QUANTIDADE_FILOSOFOS; i++) { // Loop para aguardar todos os filósofos finalizarem.
            filosofos[i].join(); // Aguarda o término da thread de cada filósofo para finalizar o programa com tudo concluído.
        }
        System.out.println("Todos os filósofos terminaram. Programa encerrado sem deadlock."); // Imprime que terminou com sucesso e sem travamento.
        scanner.close(); // Fecha o Scanner para liberar o recurso do System.in.
    }
}

