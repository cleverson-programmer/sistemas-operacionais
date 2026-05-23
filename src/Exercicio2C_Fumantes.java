/*
 * TRABALHO DE SISTEMAS OPERACIONAIS - PARTE II (THREADS)
 * EXERCÍCIO 2(c) - PROBLEMA DOS FUMANTES
 *
 * INTEGRANTES DO GRUPO:
 * - Rafael Lopes
 * - Cleverson Resende
 * - Matheus Barbosa
 * - Bernado Melgaço
 *
 * ENTRADA - PARÂMETROS DA SIMULAÇÃO:
 * 1) Quantidade de rodadas (quantas vezes o agente colocará ingredientes)
 * 2) Tempo mínimo para fumar (ms)
 * 3) Tempo máximo para fumar (ms)
 *
 * ENTRADA CASOS DE TESTE:
 * Caso 1 (pequeno):
 * rodadas=10, fumarMin=100, fumarMax=300
 *
 * Caso 2 (médio):
 * rodadas=50, fumarMin=100, fumarMax=400
 *
 * Caso 3 (dificil):
 * rodadas=200, fumarMin=10, fumarMax=80
 *
 * COMO COMPILAR (NA RAIZ DO PROJETO):
 * javac -d out src/Exercicio2C_Fumantes.java
 *
 * COMO EXECUTAR (NA RAIZ DO PROJETO):
 * java -cp out Exercicio2C_Fumantes
 */
import java.util.Random; // Importa Random para o agente escolher ingredientes aleatoriamente e para sortear tempos de fumar.
import java.util.Scanner; // Importa Scanner para ler os parâmetros digitados no teclado.
import java.util.concurrent.Semaphore; // Importa Semaphore para sincronizar agente e fumantes sem espera ocupada (busy wait).

public class Exercicio2C_Fumantes { // Declara a classe principal do exercício com main para execução direta.

    private enum Ingrediente { // Declara um enum para representar os ingredientes, facilitando imprimir e comparar.
        TABACO, // Representa o ingrediente tabaco.
        PAPEL, // Representa o ingrediente papel.
        FOSFOROS // Representa o ingrediente fósforos.
    }

    private static final class Mesa { // Declara uma classe para centralizar o estado e os semáforos compartilhados da mesa/rodadas.

        private final Semaphore agentePodeColocar; // Semáforo que controla quando o agente pode colocar itens (só após fumante terminar).
        private final Semaphore fumanteTabaco; // Semáforo para liberar o fumante que possui tabaco (quando faltam tabaco).
        private final Semaphore fumantePapel; // Semáforo para liberar o fumante que possui papel (quando faltam papel).
        private final Semaphore fumanteFosforos; // Semáforo para liberar o fumante que possui fósforos (quando faltam fósforos).

        private final Semaphore mutex; // Semáforo de exclusão mútua para proteger o estado "itensNaMesa" usado apenas para impressão.

        private Ingrediente item1; // Guarda o primeiro ingrediente colocado na mesa na rodada atual (para log em tela).
        private Ingrediente item2; // Guarda o segundo ingrediente colocado na mesa na rodada atual (para log em tela).
        private boolean mesaTemItens; // Flag para indicar se há itens na mesa nesta rodada, usada para imprimir estados coerentes.

        private Mesa() { // Construtor que inicializa os semáforos e o estado inicial.
            this.agentePodeColocar = new Semaphore(1, true); // Começa com 1 para permitir que o agente coloque na primeira rodada.
            this.fumanteTabaco = new Semaphore(0, true); // Começa com 0 porque nenhum fumante pode agir antes do agente escolher ingredientes.
            this.fumantePapel = new Semaphore(0, true); // Começa com 0 porque nenhum fumante pode agir antes do agente escolher ingredientes.
            this.fumanteFosforos = new Semaphore(0, true); // Começa com 0 porque nenhum fumante pode agir antes do agente escolher ingredientes.
            this.mutex = new Semaphore(1, true); // Mutex começa liberado para proteger atualizações do estado da mesa.
            this.item1 = null; // Inicialmente não há item 1 na mesa.
            this.item2 = null; // Inicialmente não há item 2 na mesa.
            this.mesaTemItens = false; // Inicialmente a mesa está vazia.
        }

        private void colocarItens(Ingrediente a, Ingrediente b) throws InterruptedException { // Método para o agente registrar e imprimir os itens colocados na mesa.
            mutex.acquire(); // Entra na região crítica para atualizar o estado da mesa com segurança.
            item1 = a; // Define o primeiro item da mesa para a rodada atual.
            item2 = b; // Define o segundo item da mesa para a rodada atual.
            mesaTemItens = true; // Marca que a mesa possui itens disponíveis para o fumante correto pegar.
            System.out.println("Agente colocou na mesa: " + item1 + " e " + item2 + "."); // Imprime exatamente quais ingredientes foram colocados para acompanhar a simulação.
            mutex.release(); // Sai da região crítica liberando o mutex.
        }

        private void retirarItens(String nomeFumante) throws InterruptedException { // Método para o fumante correto remover os itens e imprimir a ação.
            mutex.acquire(); // Entra na região crítica para ler/alterar o estado da mesa com segurança.
            if (mesaTemItens) { // Confere se a mesa realmente tem itens antes de remover (para log consistente).
                System.out.println(nomeFumante + " retirou da mesa os itens: " + item1 + " e " + item2 + " para montar o cigarro."); // Imprime quais itens foram retirados e o motivo (montar o cigarro).
            } else { // Caso raro: se não tiver itens por algum bug, imprime que não havia nada (ajuda em depuração).
                System.out.println(nomeFumante + " tentou retirar itens, mas a mesa estava vazia (isso não deveria acontecer)."); // Imprime situação inesperada.
            }
            item1 = null; // Limpa o item 1 para representar que ele foi removido.
            item2 = null; // Limpa o item 2 para representar que ele foi removido.
            mesaTemItens = false; // Marca que a mesa ficou vazia após o fumante pegar os itens.
            mutex.release(); // Sai da região crítica liberando o mutex.
        }
    }

    private static final class Agente extends Thread { // Declara a thread do agente, que escolhe itens e libera o fumante correspondente.

        private final Mesa mesa; // Guarda a referência para a mesa compartilhada (semáforos e estado).
        private final int rodadas; // Guarda quantas rodadas o agente deve executar.
        private final Random random; // Guarda o Random para escolher pares de ingredientes aleatórios em cada rodada.

        private Agente(Mesa mesa, int rodadas, Random random) { // Construtor do agente com parâmetros da simulação.
            this.mesa = mesa; // Armazena a mesa para poder colocar itens e liberar fumantes.
            this.rodadas = rodadas; // Armazena quantas rodadas o agente fará.
            this.random = random; // Armazena o Random para selecionar ingredientes de forma aleatória.
            setName("Agente"); // Define nome da thread para facilitar identificar prints.
        }

        private Ingrediente sortearIngrediente() { // Método auxiliar para sortear um ingrediente aleatório entre os 3 possíveis.
            int v = random.nextInt(3); // Sorteia um número 0..2 para mapear para um ingrediente.
            if (v == 0) { // Se o valor sorteado for 0, escolhe TABACO.
                return Ingrediente.TABACO; // Retorna TABACO como o ingrediente sorteado.
            }
            if (v == 1) { // Se o valor sorteado for 1, escolhe PAPEL.
                return Ingrediente.PAPEL; // Retorna PAPEL como o ingrediente sorteado.
            }
            return Ingrediente.FOSFOROS; // Caso contrário (v==2), retorna FOSFOROS como o ingrediente sorteado.
        }

        private Ingrediente ingredienteFaltante(Ingrediente a, Ingrediente b) { // Método auxiliar para descobrir qual ingrediente NÃO está na mesa.
            if ((a != Ingrediente.TABACO) && (b != Ingrediente.TABACO)) { // Se nenhum dos dois itens for TABACO, então o faltante é TABACO.
                return Ingrediente.TABACO; // Retorna TABACO como ingrediente faltante.
            }
            if ((a != Ingrediente.PAPEL) && (b != Ingrediente.PAPEL)) { // Se nenhum dos dois itens for PAPEL, então o faltante é PAPEL.
                return Ingrediente.PAPEL; // Retorna PAPEL como ingrediente faltante.
            }
            return Ingrediente.FOSFOROS; // Caso contrário, o ingrediente faltante é FOSFOROS.
        }

        @Override
        public void run() { // Implementa o comportamento do agente ao longo das rodadas.
            try { // Inicia bloco try para tratar InterruptedException em semáforos.
                for (int r = 1; r <= rodadas; r++) { // Loop principal: cada iteração é uma rodada de colocar itens e liberar um fumante.
                    mesa.agentePodeColocar.acquire(); // Espera o fumante da rodada anterior terminar de fumar para permitir nova colocação.
                    System.out.println("Agente iniciando a rodada " + r + " e escolhendo dois ingredientes diferentes."); // Imprime início da rodada e o que o agente fará.
                    Ingrediente a = sortearIngrediente(); // Sorteia o primeiro ingrediente.
                    Ingrediente b = sortearIngrediente(); // Sorteia o segundo ingrediente.
                    while (b == a) { // Enquanto o segundo ingrediente for igual ao primeiro, sorteia novamente para garantir dois diferentes.
                        b = sortearIngrediente(); // Sorteia novamente o segundo ingrediente para garantir que sejam diferentes.
                    }
                    mesa.colocarItens(a, b); // Registra e imprime os itens colocados na mesa.
                    Ingrediente faltante = ingredienteFaltante(a, b); // Descobre qual ingrediente está faltando, ou seja, qual fumante deve ser liberado.
                    System.out.println("Agente vai liberar o fumante que possui: " + faltante + " (pois este é o ingrediente faltante)."); // Imprime qual fumante será liberado e por quê.
                    if (faltante == Ingrediente.TABACO) { // Se o ingrediente faltante for TABACO, libera o fumante que tem TABACO.
                        mesa.fumanteTabaco.release(); // Libera a thread do fumante com TABACO para retirar itens, montar e fumar.
                    } else if (faltante == Ingrediente.PAPEL) { // Se o ingrediente faltante for PAPEL, libera o fumante que tem PAPEL.
                        mesa.fumantePapel.release(); // Libera a thread do fumante com PAPEL para retirar itens, montar e fumar.
                    } else { // Caso contrário, o ingrediente faltante é FOSFOROS, então libera o fumante que tem FOSFOROS.
                        mesa.fumanteFosforos.release(); // Libera a thread do fumante com FOSFOROS para retirar itens, montar e fumar.
                    }
                }
                System.out.println("Agente terminou todas as rodadas e não colocará mais ingredientes."); // Imprime que o agente concluiu todas as rodadas planejadas.
            } catch (InterruptedException e) { // Captura interrupção caso a thread do agente seja interrompida.
                System.out.println("Agente foi interrompido e vai encerrar."); // Imprime que o agente foi interrompido e encerrará.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção para boas práticas.
            }
        }
    }

    private static final class Fumante extends Thread { // Declara a thread de um fumante genérico, parametrizado pelo ingrediente que ele possui.

        private final Ingrediente possui; // Guarda qual ingrediente este fumante possui permanentemente.
        private final Mesa mesa; // Guarda a referência para a mesa, onde ele aguardará liberação e retirará itens.
        private final int rodadas; // Guarda quantas rodadas totais existem, para o fumante tentar participar quando for selecionado.
        private final int fumarMinMs; // Guarda o tempo mínimo de fumar, para simular uma duração aleatória.
        private final int fumarMaxMs; // Guarda o tempo máximo de fumar, para simular uma duração aleatória.
        private final Random random; // Guarda o Random para sortear tempos de fumar.

        private int vezesFumou; // Contador de quantas vezes este fumante foi escolhido e fumou, para imprimir resumo final.

        private Fumante(Ingrediente possui, Mesa mesa, int rodadas, int fumarMinMs, int fumarMaxMs, Random random) { // Construtor do fumante com todos os parâmetros.
            this.possui = possui; // Armazena o ingrediente que o fumante possui.
            this.mesa = mesa; // Armazena a referência da mesa para usar semáforos e retirar itens.
            this.rodadas = rodadas; // Armazena o número de rodadas para o fumante saber quando encerrar (via interrupção/stop controlado).
            this.fumarMinMs = fumarMinMs; // Armazena o tempo mínimo de fumar para sortear.
            this.fumarMaxMs = fumarMaxMs; // Armazena o tempo máximo de fumar para sortear.
            this.random = random; // Armazena o Random para sortear tempos.
            this.vezesFumou = 0; // Inicializa contador de vezes que fumou como zero.
            setName("Fumante-" + possui); // Define nome da thread para facilitar a leitura dos logs.
        }

        private int sortearTempoFumar() { // Método auxiliar para sortear o tempo de fumar no intervalo definido.
            if (fumarMaxMs <= fumarMinMs) { // Se max <= min, não há intervalo válido; usa o mínimo.
                return fumarMinMs; // Retorna o mínimo para evitar erro no nextInt e manter comportamento previsível.
            }
            return fumarMinMs + random.nextInt((fumarMaxMs - fumarMinMs) + 1); // Retorna um valor aleatório entre min e max (inclusive).
        }

        private Semaphore semaforoQueMeLibera() { // Método para escolher qual semáforo este fumante espera, baseado no ingrediente que ele possui.
            if (possui == Ingrediente.TABACO) { // Se este fumante possui TABACO, ele espera no semáforo fumanteTabaco.
                return mesa.fumanteTabaco; // Retorna o semáforo usado para liberar o fumante com TABACO.
            }
            if (possui == Ingrediente.PAPEL) { // Se este fumante possui PAPEL, ele espera no semáforo fumantePapel.
                return mesa.fumantePapel; // Retorna o semáforo usado para liberar o fumante com PAPEL.
            }
            return mesa.fumanteFosforos; // Caso contrário, este fumante possui FOSFOROS e espera no semáforo fumanteFosforos.
        }

        @Override
        public void run() { // Implementa o comportamento do fumante, aguardando ser liberado e executando montar/fumar.
            String nome = getName(); // Obtém o nome da thread para usar como prefixo em todas as mensagens.
            Semaphore meuSemaforo = semaforoQueMeLibera(); // Obtém o semáforo em que este fumante ficará bloqueado até ser escolhido.
            try { // Inicia bloco try para tratar InterruptedException.
                while (true) { // Loop contínuo: o fumante só age quando for liberado pelo agente e encerra quando for interrompido no final.
                    System.out.println(nome + " possui " + possui + " e está bloqueado aguardando o agente liberar sua vez."); // Imprime que o fumante está esperando ser escolhido.
                    meuSemaforo.acquire(); // Bloqueia até o agente liberar este fumante (quando o ingrediente dele for o faltante).
                    vezesFumou++; // Incrementa o contador porque ele foi escolhido e vai fumar uma vez nesta rodada.
                    mesa.retirarItens(nome); // Retira os itens da mesa para montar o cigarro, imprimindo quais foram retirados.
                    System.out.println(nome + " está montando o cigarro usando os dois ingredientes da mesa + o ingrediente que ele já possui (" + possui + ")."); // Imprime a montagem do cigarro para explicar o processo.
                    int tempo = sortearTempoFumar(); // Sorteia por quanto tempo este fumante vai fumar para simular duração aleatória.
                    System.out.println(nome + " começou a fumar no fumódromo por " + tempo + " ms."); // Imprime que ele começou a fumar e quanto tempo vai durar.
                    if (tempo > 0) { // Verifica se o tempo é positivo para evitar sleep desnecessário.
                        Thread.sleep(tempo); // Dorme para simular o tempo fumando, mantendo o agente esperando até terminar.
                    }
                    System.out.println(nome + " terminou de fumar e vai liberar o agente para a próxima rodada."); // Imprime que terminou de fumar e que vai acordar/liberar o agente.
                    mesa.agentePodeColocar.release(); // Libera o agente para colocar novos ingredientes e iniciar a próxima rodada.
                }
            } catch (InterruptedException e) { // Captura interrupção quando a simulação terminar e a thread for interrompida pelo main.
                System.out.println(nome + " foi interrompido e encerrará. Total de vezes que fumou: " + vezesFumou + "."); // Imprime que encerrou e quantas vezes foi escolhido.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção para manter boas práticas.
            }
        }
    }

    private static int lerInteiro(Scanner scanner, String mensagem) { // Declara método utilitário para ler inteiro com validação para evitar travamentos por entrada inválida.
        System.out.print(mensagem); // Imprime a mensagem que orienta o usuário sobre o valor a ser digitado.
        while (!scanner.hasNextInt()) { // Enquanto não houver um inteiro válido, continua pedindo entrada.
            System.out.println("Valor inválido. Digite um número inteiro."); // Informa que o valor digitado não é inteiro e pede correção.
            scanner.next(); // Descarta o token inválido para poder tentar novamente.
            System.out.print(mensagem); // Reimprime a mensagem para solicitar novamente o mesmo valor.
        }
        return scanner.nextInt(); // Retorna o inteiro lido, que será usado como parâmetro da simulação.
    }

    public static void main(String[] args) throws Exception { // Declara o main para ler entrada, iniciar threads e finalizar a simulação.
        System.out.println("Integrantes do grupo: Rafael Lopes, Cleverson Resende, Matheus Barbosa, Bernado Melgaço."); // Imprime os integrantes conforme exigido no enunciado.
        System.out.println("Exercício 2(c) - Problema dos Fumantes (Threads + Semáforos)."); // Imprime o título do programa para contextualizar o que está sendo executado.
        Scanner scanner = new Scanner(System.in); // Cria Scanner para ler os valores digitados pelo usuário.
        int rodadas = lerInteiro(scanner, "Digite a quantidade de rodadas: "); // Lê quantas rodadas o agente deve executar.
        int fumarMin = lerInteiro(scanner, "Digite o tempo mínimo para fumar (ms): "); // Lê o tempo mínimo de fumar para o sleep.
        int fumarMax = lerInteiro(scanner, "Digite o tempo máximo para fumar (ms): "); // Lê o tempo máximo de fumar para o sleep.
        Random random = new Random(); // Cria Random para escolher ingredientes e tempo de fumar.
        Mesa mesa = new Mesa(); // Cria a mesa compartilhada com semáforos e estado para impressão.
        Fumante fumanteTabaco = new Fumante(Ingrediente.TABACO, mesa, rodadas, fumarMin, fumarMax, random); // Cria o fumante que possui TABACO e só é liberado quando TABACO for o ingrediente faltante.
        Fumante fumantePapel = new Fumante(Ingrediente.PAPEL, mesa, rodadas, fumarMin, fumarMax, random); // Cria o fumante que possui PAPEL e só é liberado quando PAPEL for o ingrediente faltante.
        Fumante fumanteFosforos = new Fumante(Ingrediente.FOSFOROS, mesa, rodadas, fumarMin, fumarMax, random); // Cria o fumante que possui FOSFOROS e só é liberado quando FOSFOROS for o ingrediente faltante.
        Agente agente = new Agente(mesa, rodadas, random); // Cria o agente que escolherá ingredientes e liberará o fumante correto em cada rodada.
        fumanteTabaco.start(); // Inicia a thread do fumante que tem TABACO para ele ficar bloqueado aguardando ser liberado.
        fumantePapel.start(); // Inicia a thread do fumante que tem PAPEL para ele ficar bloqueado aguardando ser liberado.
        fumanteFosforos.start(); // Inicia a thread do fumante que tem FOSFOROS para ele ficar bloqueado aguardando ser liberado.
        agente.start(); // Inicia a thread do agente para ele começar a colocar ingredientes e liberar fumantes por "rodadas".
        agente.join(); // Aguarda o agente terminar todas as rodadas, garantindo que não haverá novas liberações após esse ponto.
        System.out.println("Main: agente terminou. Interrompendo fumantes para encerrar o programa sem travar."); // Imprime que o agente acabou e que os fumantes serão encerrados por interrupção.
        fumanteTabaco.interrupt(); // Interrompe o fumante TABACO para ele sair do acquire e encerrar o loop.
        fumantePapel.interrupt(); // Interrompe o fumante PAPEL para ele sair do acquire e encerrar o loop.
        fumanteFosforos.interrupt(); // Interrompe o fumante FOSFOROS para ele sair do acquire e encerrar o loop.
        fumanteTabaco.join(); // Aguarda o fumante TABACO encerrar para garantir fechamento limpo.
        fumantePapel.join(); // Aguarda o fumante PAPEL encerrar para garantir fechamento limpo.
        fumanteFosforos.join(); // Aguarda o fumante FOSFOROS encerrar para garantir fechamento limpo.
        System.out.println("Programa encerrado após concluir " + rodadas + " rodadas."); // Imprime a mensagem final informando que a simulação terminou.
        scanner.close(); // Fecha o Scanner para liberar o recurso de entrada padrão.
    }
}

