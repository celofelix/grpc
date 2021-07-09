package br.com.zup.edu

import com.google.protobuf.Any
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FretesGrpcEndpoint : FretesServiceGrpc.FretesServiceImplBase() {

    private val logger = LoggerFactory.getLogger(FretesGrpcEndpoint::class.java)

    override fun calculaFrete(request: CalculaFreteRequest?, responseObserver: StreamObserver<CalculaFreteResponse>?) {

        logger.info("Request Frete -> ${request?.cep}")

        if (request?.cep.isNullOrBlank()) {
            val e = Status.INVALID_ARGUMENT
                .withDescription("O CEP deve ser preenchido")
                .asRuntimeException()

            responseObserver?.onError(e)
        }

        if (!request?.cep!!.matches("[0-9]{5}-[0-9]{3}".toRegex())) {
            val e = Status.INVALID_ARGUMENT
                .withDescription("O CEP está inválido")
                .augmentDescription("O CEP deve possuir o formato: 00000-000")
                .asRuntimeException()

            responseObserver?.onError(e)
        }

        if (request.cep.endsWith("333")) {

            val statusProto = com.google.rpc.Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED.number)
                .setMessage("Usuário não possuir permissão para acessar o recurso")
                .addDetails(Any.pack(ErrorDetails.newBuilder()
                    .setCode(Status.PERMISSION_DENIED.code.value())
                    .setMessage("Token expirado")
                    .build())
                )
                .build()

            val runtimeException = StatusProto.toStatusRuntimeException(statusProto)

            responseObserver?.onError(runtimeException)
        }

        val valorFrete = Random.nextDouble(from = 0.0, until = 150.00)
        try {
            if (valorFrete > 100.0) {
                logger.info("Valor Frete -> $valorFrete")
                throw IllegalStateException("Erro inesperado ao calcular o frete, por favor tente novamente")
            }
        } catch (e: Exception) {
             
            responseObserver?.onError(
                Status.INTERNAL
                    .withDescription(e.message)
                    .withCause(e)
                    .asRuntimeException())
        }

        return CalculaFreteResponse.newBuilder()
            .setValor(valorFrete)
            .setCep(request.cep)
            .build()
            .let {
                logger.info("Response Frete -> $it")

                responseObserver?.onNext(it)
                responseObserver?.onCompleted()
            }
    }
}