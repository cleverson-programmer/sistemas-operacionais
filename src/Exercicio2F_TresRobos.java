/*
 * TRABALHO DE SISTEMAS OPERACIONAIS - PARTE II (THREADS)
 * EXERCÍCIO 2(f) - PROBLEMA DOS TRÊS ROBÔS
 *
 * INTEGRANTES DO GRUPO:
 * - Rafael Lopes
 * - Cleverson Resende
 * - Matheus Barbosa
 * - Bernado Melgaço
 *
 * ENTRADA - PARÂMETROS DA SIMULAÇÃO:
 * 1) Quantidade de ciclos (um ciclo = Bart → Lisa → Maggie → Lisa)
 * 2) Tempo mínimo do move() (ms)
 * 3) Tempo máximo do move() (ms)
 *
 * ENTRADA CASOS DE TESTE:
 * Caso 1 (pequeno):
 * ciclos=3, moveMin=10, moveMax=50
 *
 * Caso 2 (médio):
 * ciclos=10, moveMin=10, moveMax=100
 *
 * Caso 3 (dificil):
 * ciclos=100, moveMin=0, moveMax=10
 *
 * COMO COMPILAR (NA RAIZ DO PROJETO):
 * javac -d out src/Exercicio2F_TresRobos.java
 *
 * COMO EXECUTAR (NA RAIZ DO PROJETO):
 * java -cp out Exercicio2F_TresRobos
 */
import java.util.Random; // Importa Random para sortear a duração do move() e simular tempos diferentes de execução.
import java.util.Scanner; // Importa Scanner para ler os parâmetros digitados no teclado.
import java.util.concurrent.Semaphore; // Importa Semaphore para sincronizar a sequência entre robôs sem busy wait.

public class Exercicio2F_TresRobos { // Declara a classe principal que contém main e as threads dos robôs.

    private static final class Controlador { // Declara uma classe interna para armazenar semáforos e parâmetros compartilhados.

        private final Semaphore semBart; // Semáforo que libera o Bart para mover quando for a vez dele.
        private final Semaphore semLisaDepoisBart; // Semáforo que libera a Lisa após o Bart.
        private final Semaphore semMaggie; // Semáforo que libera a Maggie após a Lisa (primeira vez da Lisa).
        private final Semaphore semLisaDepoisMaggie; // Semáforo que libera a Lisa após a Maggie (segunda vez da Lisa).

        private final int ciclos; // Guarda quantos ciclos completos devem ser executados (Bart→Lisa→Maggie→Lisa).
        private final int moveMinMs; // Guarda o tempo mínimo do move() para sorteio.
        private final int moveMaxMs; // Guarda o tempo máximo do move() para sorteio.

        private final Random random; // Random para sortear tempos do move() no intervalo definido.

        private int movimentosExecutados; // Contador de movimentos totais executados, para imprimir um resumo ao final.

        private Controlador(int ciclos, int moveMinMs, int moveMaxMs, Random random) { // Construtor que recebe parâmetros e inicializa semáforos.
            this.semBart = new Semaphore(1, true); // Inicializa com 1 para o Bart começar a sequência imediatamente.
            this.semLisaDepoisBart = new Semaphore(0, true); // Inicializa com 0 porque a Lisa só pode mover após o Bart.
            this.semMaggie = new Semaphore(0, true); // Inicializa com 0 porque a Maggie só pode mover após a Lisa (primeira Lisa).
            this.semLisaDepoisMaggie = new Semaphore(0, true); // Inicializa com 0 porque a Lisa (segunda vez) só pode mover após a Maggie.
            this.ciclos = ciclos; // Armazena quantos ciclos rodar, para encerrar em tempo finito.
            this.moveMinMs = moveMinMs; // Armazena o mínimo para sortear duração do move().
            this.moveMaxMs = moveMaxMs; // Armazena o máximo para sortear duração do move().
            this.random = random; // Armazena o Random para sorteios.
            this.movimentosExecutados = 0; // Inicializa contador total de movimentos como 0.
        }

        private int sortearTempoMove() { // Método auxiliar para sortear o tempo de move no intervalo definido.
            if (moveMaxMs <= moveMinMs) { // Se max <= min, usa o mínimo para evitar erro e manter previsibilidade.
                return moveMinMs; // Retorna o mínimo como tempo fixo do move().
            }
            return moveMinMs + random.nextInt((moveMaxMs - moveMinMs) + 1); // Retorna valor aleatório no intervalo [min, max].
        }

        private void move(String nomeRobo, int ciclo, String passo) throws InterruptedException { // Método exigido no enunciado para indicar movimento do robô.
            int tempo = sortearTempoMove(); // Sorteia um tempo para simular que o robô leva um tempo para mover.
            movimentosExecutados++; // Incrementa o contador total de movimentos executados para resumo final.
            System.out.println("Ciclo " + ciclo + " - " + passo + ": " + nomeRobo + " executou move() (duração: " + tempo + " ms)."); // Imprime exatamente qual robô moveu, em qual ponto da sequência, e o tempo do movimento.
            if (tempo > 0) { // Verifica se o tempo é maior que zero para evitar sleep desnecessário.
                Thread.sleep(tempo); // Pausa para simular o tempo do movimento, ajudando a visualizar concorrência sem quebrar a ordem.
            }
        }
    }

    private static final class RoboBart extends Thread { // Declara a thread do robô Bart, que deve mover primeiro em cada ciclo.

        private final Controlador controlador; // Referência para acessar semáforos e parâmetros.

        private RoboBart(Controlador controlador) { // Construtor que recebe o controlador compartilhado.
            this.controlador = controlador; // Armazena o controlador para usar semáforos e o método move().
            setName("Bart"); // Define o nome da thread para facilitar identificar prints.
        }

        @Override
        public void run() { // Implementa a sequência de movimentos do Bart.
            String nome = getName(); // Obtém o nome da thread para usar nas mensagens.
            try { // Inicia bloco try para tratar InterruptedException.
                for (int ciclo = 1; ciclo <= controlador.ciclos; ciclo++) { // Loop por ciclos, pois Bart move exatamente 1 vez por ciclo.
                    controlador.semBart.acquire(); // Bloqueia até ser a vez do Bart (no início já está liberado).
                    controlador.move(nome, ciclo, "Passo 1 (Bart)"); // Executa o move() do Bart e imprime na tela o que aconteceu.
                    controlador.semLisaDepoisBart.release(); // Libera a Lisa para executar o próximo passo da sequência.
                }
                System.out.println(nome + " terminou seus movimentos e encerrará sua thread."); // Imprime que o Bart terminou após completar todos os ciclos.
            } catch (InterruptedException e) { // Captura interrupção se algo interromper a thread.
                System.out.println(nome + " foi interrompido e encerrará."); // Imprime que o Bart foi interrompido.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção como boa prática.
            }
        }
    }

    private static final class RoboLisa extends Thread { // Declara a thread da Lisa, que move duas vezes por ciclo (após Bart e após Maggie).

        private final Controlador controlador; // Referência para acessar semáforos e parâmetros.

        private RoboLisa(Controlador controlador) { // Construtor que recebe o controlador compartilhado.
            this.controlador = controlador; // Armazena o controlador para usar semáforos e o método move().
            setName("Lisa"); // Define o nome da thread para facilitar identificar prints.
        }

        @Override
        public void run() { // Implementa os movimentos da Lisa em cada ciclo, em duas etapas.
            String nome = getName(); // Obtém o nome da thread para usar nas mensagens.
            try { // Inicia bloco try para tratar InterruptedException.
                for (int ciclo = 1; ciclo <= controlador.ciclos; ciclo++) { // Loop por ciclos, pois a Lisa move duas vezes em cada ciclo.
                    controlador.semLisaDepoisBart.acquire(); // Espera ser liberada após o Bart (primeira vez da Lisa no ciclo).
                    controlador.move(nome, ciclo, "Passo 2 (Lisa após Bart)"); // Executa o move() da Lisa e imprime que foi após Bart.
                    controlador.semMaggie.release(); // Libera a Maggie para executar o próximo passo da sequência.
                    controlador.semLisaDepoisMaggie.acquire(); // Espera ser liberada após a Maggie (segunda vez da Lisa no ciclo).
                    controlador.move(nome, ciclo, "Passo 4 (Lisa após Maggie)"); // Executa o move() da Lisa novamente e imprime que foi após Maggie.
                    controlador.semBart.release(); // Libera o Bart para iniciar o próximo ciclo.
                }
                System.out.println(nome + " terminou seus movimentos e encerrará sua thread."); // Imprime que a Lisa terminou após completar todos os ciclos.
            } catch (InterruptedException e) { // Captura interrupção se algo interromper a thread.
                System.out.println(nome + " foi interrompido e encerrará."); // Imprime que a Lisa foi interrompida.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção como boa prática.
            }
        }
    }

    private static final class RoboMaggie extends Thread { // Declara a thread da Maggie, que move uma vez por ciclo após a Lisa (primeira Lisa).

        private final Controlador controlador; // Referência para acessar semáforos e parâmetros.

        private RoboMaggie(Controlador controlador) { // Construtor que recebe o controlador compartilhado.
            this.controlador = controlador; // Armazena o controlador para usar semáforos e o método move().
            setName("Maggie"); // Define o nome da thread para facilitar identificar prints.
        }

        @Override
        public void run() { // Implementa a sequência de movimentos da Maggie.
            String nome = getName(); // Obtém o nome da thread para usar nas mensagens.
            try { // Inicia bloco try para tratar InterruptedException.
                for (int ciclo = 1; ciclo <= controlador.ciclos; ciclo++) { // Loop por ciclos, pois Maggie move exatamente 1 vez por ciclo.
                    controlador.semMaggie.acquire(); // Bloqueia até ser a vez da Maggie (liberada após a Lisa no passo 2).
                    controlador.move(nome, ciclo, "Passo 3 (Maggie)"); // Executa o move() da Maggie e imprime na tela o que aconteceu.
                    controlador.semLisaDepoisMaggie.release(); // Libera a Lisa para executar o passo final do ciclo (Lisa após Maggie).
                }
                System.out.println(nome + " terminou seus movimentos e encerrará sua thread."); // Imprime que a Maggie terminou após completar todos os ciclos.
            } catch (InterruptedException e) { // Captura interrupção se algo interromper a thread.
                System.out.println(nome + " foi interrompido e encerrará."); // Imprime que a Maggie foi interrompida.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção como boa prática.
            }
        }
    }

    private static int lerInteiro(Scanner scanner, String mensagem) { // Método utilitário para ler um inteiro com validação, evitando travar por entrada inválida.
        System.out.print(mensagem); // Imprime a mensagem para orientar o usuário sobre o que deve ser digitado.
        while (!scanner.hasNextInt()) { // Enquanto não houver um inteiro válido na entrada, continua solicitando.
            System.out.println("Valor inválido. Digite um número inteiro."); // Informa ao usuário que a entrada não é válida e pede correção.
            scanner.next(); // Descarta o token inválido para tentar novamente.
            System.out.print(mensagem); // Reimprime a mensagem original para solicitar o valor correto.
        }
        return scanner.nextInt(); // Retorna o inteiro válido digitado, que será usado como parâmetro da simulação.
    }

    public static void main(String[] args) throws Exception { // Método main que lê parâmetros, cria threads e executa a sequência exigida.
        System.out.println("Integrantes do grupo: Rafael Lopes, Cleverson Resende, Matheus Barbosa, Gustavo Cicero, Bernado Melgaço."); // Imprime os nomes do grupo conforme exigido.
        System.out.println("Exercício 2(f) - Problema dos três robôs (Threads + Semáforos)."); // Imprime o título do programa para contextualizar.
        Scanner scanner = new Scanner(System.in); // Cria Scanner para ler valores digitados pelo usuário.
        int ciclos = lerInteiro(scanner, "Digite a quantidade de ciclos (Bart→Lisa→Maggie→Lisa): "); // Lê quantos ciclos completos executar.
        int moveMin = lerInteiro(scanner, "Digite o tempo mínimo do move() (ms): "); // Lê o tempo mínimo do move para sorteio.
        int moveMax = lerInteiro(scanner, "Digite o tempo máximo do move() (ms): "); // Lê o tempo máximo do move para sorteio.
        Random random = new Random(); // Cria Random para sortear tempos do move().
        Controlador controlador = new Controlador(ciclos, moveMin, moveMax, random); // Cria o controlador com semáforos inicializados e parâmetros.
        Thread bart = new RoboBart(controlador); // Cria a thread do Bart, que inicia a sequência.
        Thread lisa = new RoboLisa(controlador); // Cria a thread da Lisa, que move duas vezes por ciclo.
        Thread maggie = new RoboMaggie(controlador); // Cria a thread da Maggie, que move uma vez por ciclo.
        bart.start(); // Inicia a thread do Bart para executar o passo 1 do primeiro ciclo.
        lisa.start(); // Inicia a thread da Lisa para aguardar sua vez e executar passos 2 e 4.
        maggie.start(); // Inicia a thread da Maggie para aguardar sua vez e executar passo 3.
        bart.join(); // Aguarda o Bart terminar todos os ciclos para garantir que a simulação encerre corretamente.
        lisa.join(); // Aguarda a Lisa terminar todos os ciclos para garantir que a simulação encerre corretamente.
        maggie.join(); // Aguarda a Maggie terminar todos os ciclos para garantir que a simulação encerre corretamente.
        System.out.println("Resumo final: movimentos executados = " + controlador.movimentosExecutados + " (esperado: " + (ciclos * 4) + ")."); // Imprime quantos moves foram feitos e qual era o esperado.
        System.out.println("Programa encerrado mantendo a sequência obrigatória sem busy wait."); // Imprime que terminou e reforça que a sequência foi respeitada.
        scanner.close(); // Fecha o Scanner para liberar o recurso de entrada padrão.
    }
}

