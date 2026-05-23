/*
 * TRABALHO DE SISTEMAS OPERACIONAIS - PARTE II (THREADS)
 * EXERCÍCIO 2(a) - PROBLEMA DO BARBEIRO DORMINHOCO
 *
 * INTEGRANTES DO GRUPO:
 * - Rafael Lopes
 * - Cleverson Resende
 * - Matheus Barbosa
 * - Bernado Melgaço
 *
 * OBJETIVO:
 * Simular uma barbearia com 1 barbeiro, 1 cadeira de barbeiro e N cadeiras de espera.
 * Usar threads para simular concorrência e semáforos para evitar condições de corrida.
 *
 * ENTRADA - PARÂMETROS DA SIMULAÇÃO:
 * 1) Número de cadeiras de espera (N)
 * 2) Quantidade de clientes a serem gerados
 * 3) Tempo mínimo do corte (ms)
 * 4) Tempo máximo do corte (ms)
 * 5) Tempo mínimo entre chegadas de clientes (ms)
 * 6) Tempo máximo entre chegadas de clientes (ms)
 *
 * ENTRADA CASOS DE TESTE:
 * Caso 1 (pequeno):
 * N=1, clientes=5, corteMin=200, corteMax=400, chegadaMin=50, chegadaMax=100
 *
 * Caso 2 (médio):
 * N=3, clientes=20, corteMin=200, corteMax=800, chegadaMin=50, chegadaMax=300
 *
 * Caso 3 (dificil):
 * N=0, clientes=10, corteMin=200, corteMax=800, chegadaMin=50, chegadaMax=150
 *
 * COMO COMPILAR (NA RAIZ DO PROJETO):
 * javac -d out src/Exercicio2A_BarbeiroDorminhoco.java
 *
 * COMO EXECUTAR (NA RAIZ DO PROJETO):
 * java -cp out Exercicio2A_BarbeiroDorminhoco
*/
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Exercicio2A_BarbeiroDorminhoco {

    private static final class Barbearia {

        private final int numeroCadeirasDeEspera;
        private final Random random;

        private final int tempoCorteMinMs;
        private final int tempoCorteMaxMs;

        private final Semaphore customers;
        private final Semaphore barbers;
        private final Semaphore mutex;

        private final Semaphore clienteNaCadeira;
        private final Semaphore corteFinalizado;

        private int waiting;
        private int clientesAtendidos;
        private int clientesForamEmbora;

        private boolean barbeariaAberta;
        private Barbearia(int numeroCadeirasDeEspera, int tempoCorteMinMs, int tempoCorteMaxMs, Random random) {
            this.numeroCadeirasDeEspera = numeroCadeirasDeEspera;
            this.tempoCorteMinMs = tempoCorteMinMs;
            this.tempoCorteMaxMs = tempoCorteMaxMs;
            this.random = random;
            this.customers = new Semaphore(0);
            this.barbers = new Semaphore(0);
            this.mutex = new Semaphore(1);
            this.clienteNaCadeira = new Semaphore(0);
            this.corteFinalizado = new Semaphore(0);
            this.waiting = 0;
            this.clientesAtendidos = 0;
            this.clientesForamEmbora = 0;
            this.barbeariaAberta = true;
        }

        private void fecharBarbearia() throws InterruptedException {
            mutex.acquire();
            barbeariaAberta = false;
            mutex.release();
            customers.release();
        }

        private boolean tentarEntrarNaEspera(int idCliente) throws InterruptedException {
            mutex.acquire();
            if (waiting >= numeroCadeirasDeEspera) {
                clientesForamEmbora++;
                System.out.println("Cliente " + idCliente + " chegou, mas não há cadeira de espera. Cliente vai embora sem cortar o cabelo.");
                mutex.release();
                return false;
            }
            waiting++;
            System.out.println("Cliente " + idCliente + " chegou e sentou na espera. Clientes esperando agora: " + waiting + ".");
            customers.release();
            mutex.release();
            return true;
        }

        private void registrarAtendimentoConcluido(int idCliente) throws InterruptedException {
            mutex.acquire();
            clientesAtendidos++;
            System.out.println("Cliente " + idCliente + " terminou o corte e saiu da barbearia.");
            mutex.release();
        }

        private int sortearTempoCorteMs() {
            if (tempoCorteMaxMs <= tempoCorteMinMs) {
                return tempoCorteMinMs;
            }
            return tempoCorteMinMs + random.nextInt((tempoCorteMaxMs - tempoCorteMinMs) + 1);
        }
    }

    private static final class Barbeiro extends Thread {

        private final Barbearia barbearia;

        private Barbeiro(Barbearia barbearia) {
            this.barbearia = barbearia;
            setName("Barbeiro");
        }

        @Override
        public void run() {
            try {
                while (true) {
                    System.out.println("Barbeiro está esperando clientes (se não houver, ele dorme bloqueado no semáforo).");
                    barbearia.customers.acquire();
                    barbearia.mutex.acquire();
                    if (!barbearia.barbeariaAberta && barbearia.waiting == 0) {
                        barbearia.mutex.release();
                        System.out.println("Barbeiro percebeu que a barbearia fechou e não há clientes esperando. Encerrando o expediente.");
                        break;
                    }
                    barbearia.waiting--;
                    System.out.println("Barbeiro chamou um cliente. Clientes ainda esperando: " + barbearia.waiting + ".");
                    barbearia.barbers.release();
                    barbearia.mutex.release();
                    barbearia.clienteNaCadeira.acquire();
                    int tempoCorte = barbearia.sortearTempoCorteMs();
                    System.out.println("Barbeiro começou a cortar o cabelo (duração: " + tempoCorte + " ms).");
                    Thread.sleep(tempoCorte);
                    System.out.println("Barbeiro terminou o corte de cabelo.");
                    barbearia.corteFinalizado.release();
                }
            } catch (InterruptedException e) {
                System.out.println("Barbeiro foi interrompido e vai encerrar.");
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class Cliente extends Thread {

        private final int idCliente;
        private final Barbearia barbearia;
        private final int atrasoChegadaMs;

        private Cliente(int idCliente, Barbearia barbearia, int atrasoChegadaMs) {
            this.idCliente = idCliente;
            this.barbearia = barbearia;
            this.atrasoChegadaMs = atrasoChegadaMs;
            setName("Cliente-" + idCliente);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(atrasoChegadaMs);
                System.out.println("Cliente " + idCliente + " chegou na barbearia após " + atrasoChegadaMs + " ms.");
                boolean vaiSerAtendido = barbearia.tentarEntrarNaEspera(idCliente);
                if (!vaiSerAtendido) {
                    return;
                }
                System.out.println("Cliente " + idCliente + " está aguardando ser chamado pelo barbeiro.");
                barbearia.barbers.acquire();
                System.out.println("Cliente " + idCliente + " foi chamado e sentou na cadeira de barbeiro.");
                barbearia.clienteNaCadeira.release();
                barbearia.corteFinalizado.acquire();
                barbearia.registrarAtendimentoConcluido(idCliente);
            } catch (InterruptedException e) {
                System.out.println("Cliente " + idCliente + " foi interrompido e vai encerrar.");
                Thread.currentThread().interrupt();
            }
        }
    }

    private static int lerInteiro(Scanner scanner, String mensagem) {
        System.out.print(mensagem);
        while (!scanner.hasNextInt()) {
            System.out.println("Valor inválido. Digite um número inteiro.");
            scanner.next();
            System.out.print(mensagem);
        }
        return scanner.nextInt();
    }

    private static int sortearEntre(Random random, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt((max - min) + 1);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Integrantes do grupo: Rafael Lopes, Cleverson Resende, Matheus Barbosa, Bernado Melgaço.");
        System.out.println("Exercício 2(a) - Problema do Barbeiro Dorminhoco (Threads + Semáforos).");
        Scanner scanner = new Scanner(System.in);
        int nCadeiras = lerInteiro(scanner, "Digite o número de cadeiras de espera (N): ");
        int qtdClientes = lerInteiro(scanner, "Digite a quantidade de clientes a gerar: ");
        int corteMin = lerInteiro(scanner, "Digite o tempo mínimo do corte (ms): ");
        int corteMax = lerInteiro(scanner, "Digite o tempo máximo do corte (ms): ");
        int chegadaMin = lerInteiro(scanner, "Digite o tempo mínimo entre chegadas de clientes (ms): ");
        int chegadaMax = lerInteiro(scanner, "Digite o tempo máximo entre chegadas de clientes (ms): ");
        Random random = new Random();
        Barbearia barbearia = new Barbearia(nCadeiras, corteMin, corteMax, random);
        Barbeiro barbeiro = new Barbeiro(barbearia);
        barbeiro.start();
        List<Thread> clientes = new ArrayList<>();
        for (int i = 1; i <= qtdClientes; i++) {
            int atraso = sortearEntre(random, chegadaMin, chegadaMax);
            Cliente cliente = new Cliente(i, barbearia, atraso);
            clientes.add(cliente);
            cliente.start();
        }
        for (Thread cliente : clientes) {
            cliente.join();
        }
        System.out.println("Todos os clientes já finalizaram (atendidos ou foram embora). Iniciando fechamento da barbearia.");
        barbearia.fecharBarbearia();
        barbeiro.join();
        System.out.println("Resumo final: clientes atendidos = " + barbearia.clientesAtendidos + ", clientes que foram embora = " + barbearia.clientesForamEmbora + ".");
        scanner.close();
    }
}
