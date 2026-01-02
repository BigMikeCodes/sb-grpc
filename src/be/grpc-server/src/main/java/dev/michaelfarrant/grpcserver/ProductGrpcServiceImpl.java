package dev.michaelfarrant.grpcserver;

import dev.michaelfarrant.grpc.generated.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class ProductGrpcServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {

    @Override
    public void searchProducts(SearchProductsRequest request, StreamObserver<SearchProductsResponse> responseObserver) {

        SearchProductsResponse.ProductSearchResult result = SearchProductsResponse.ProductSearchResult.newBuilder()
                .setId("123")
                .setName("Sample Product")
                .setImageUrl("This is a sample product description.")
                .setIsAvailable(true)
                .addTags( "sample")
                .build();

        SearchProductsResponse response = SearchProductsResponse.newBuilder()
                .addResults(result)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getProductDetails(GetProductDetailsRequest request, StreamObserver<GetProductDetailsResponse> responseObserver) {

        GetProductDetailsResponse response = GetProductDetailsResponse.newBuilder()
                .setId(request.getProductId())
                .setName("Sample Product")
                .setDescription("This is a detailed description of the sample product.")
                .setImageUrl("http://example.com/sample-product.jpg")
                .setIsAvailable(true)
                .addTags("sample")
                .addTags("product")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

}
