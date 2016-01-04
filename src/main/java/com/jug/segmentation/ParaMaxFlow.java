/**
 *
 */
package com.jug.segmentation;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.paramaxflow.Parametric;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import com.jug.fkt.Function1D;

/**
 * @author jug
 */
public class ParaMaxFlow< T extends RealType< T > > {

	Parametric parametric;

	private final RandomAccessibleInterval< T > rai;
	private Img< LongType > regionsImg;

	private final boolean is3d;

	/**
	 * Instatiates an instance of <code>Parametric</code>, and builds a graph
	 * structure corresponding to the given image.
	 * 
	 * @param rai
	 *            the image for which the graph in <code>Parametric</code> will
	 *            be built. This <code>Img</code> must be 1d, 2d, or 3d.
	 * @param probMap
	 *            if given this probability map must have the same
	 *            dimensionality as <code>img</code>. All elements must be in
	 *            [0,1] and will be used to modify the cost for unary and binary
	 *            potentials.
	 * @param is3d
	 *            if true, the given image will be treated as being 3d,
	 *            otherwise as a sequence of 2d images. In case <code>img</code>
	 *            is 1d or 2d, this parameter can be ignored.
	 * @param fktUnary
	 * @param costIsing
	 * @param fktPairwiseX
	 * @param fktPairwiseY
	 * @param fktPairwiseZ
	 */
	public ParaMaxFlow( final RandomAccessibleInterval< T > rai, final RandomAccessibleInterval< ? extends RealType > potentialModulationImage, final boolean is3d, final Function1D< Double > fktUnary, final double costIsing, final Function1D< Double > fktPairwiseX, final Function1D< Double > fktPairwiseY, final Function1D< Double > fktPairwiseZ ) {
		if ( rai.numDimensions() > 3 ) { throw new UnsupportedOperationException( "ParaMaxFlow does not yet support Img's with >3 dimensions! Sorry..." ); }
		if ( Views.iterable( rai ).size() * rai.numDimensions() > Integer.MAX_VALUE ) { throw new UnsupportedOperationException( "ParaMaxFlow can only operate on Img's with <Integer.MAX_VALUE many binary potentials!" ); }

		this.rai = Views.zeroMin( rai );
		this.regionsImg = null;

		this.is3d = is3d;

		parametric = new Parametric( ( int ) Views.iterable( rai ).size(), ( int ) ( Views.iterable( rai ).size() * rai.numDimensions() ) );
		buildGraph( fktUnary, costIsing, fktPairwiseX, fktPairwiseY, fktPairwiseZ, potentialModulationImage );
	}

	/**
	 * 
	 * @param rai
	 * @param is3d
	 * @param unaryCostFactor
	 * @param unaryPotentialImage
	 * @param isingCost
	 * @param pairwisePotentialImage
	 */
	public ParaMaxFlow( final RandomAccessibleInterval< T > rai, final boolean is3d, final double unaryCostFactor, final RandomAccessibleInterval< ? extends RealType > unaryPotentialImage, final double isingCost, final RandomAccessibleInterval< ? extends RealType > pairwisePotentialImage ) {
		if ( rai.numDimensions() > 3 ) { throw new UnsupportedOperationException( "ParaMaxFlow does not yet support Img's with >3 dimensions! Sorry..." ); }
		if ( Views.iterable( rai ).size() * rai.numDimensions() > Integer.MAX_VALUE ) { throw new UnsupportedOperationException( "ParaMaxFlow can only operate on Img's with <Integer.MAX_VALUE many binary potentials!" ); }

		// check sizes of given images (the classified images can be of higher dimensionality)
		if ( rai.numDimensions() > unaryPotentialImage.numDimensions() || rai.numDimensions() > pairwisePotentialImage.numDimensions() ) {
			throw new UnsupportedOperationException( "Potential-images must have same dimensionality as the source image!" );
		} else {
			for ( int i = 0; i < rai.numDimensions(); i++ ) {
				if ( rai.dimension( i ) != unaryPotentialImage.dimension( i ) ) { throw new UnsupportedOperationException( "Unaries-image does not match the source image size in dimension " + i + "!" ); }
				if ( rai.dimension( i ) != pairwisePotentialImage.dimension( i ) ) { throw new UnsupportedOperationException( "Unaries-image does not match the source image size in dimension " + i + "!" ); }
			}
		}

		this.rai = Views.zeroMin( rai );
		this.regionsImg = null;

		this.is3d = is3d;

		parametric = new Parametric( ( int ) Views.iterable( rai ).size(), ( int ) ( Views.iterable( rai ).size() * rai.numDimensions() ) );
		buildGraph( unaryCostFactor, unaryPotentialImage, isingCost, pairwisePotentialImage );
	}

	/**
	 * @param unaryCostFactor
	 * @param unaryPotentialImage
	 * @param isingCost
	 * @param pairwisePotentialImage
	 */
	private void buildGraph( final double unaryCostFactor, final RandomAccessibleInterval< ? extends RealType > unaryPotentialImage, final double isingCost, final RandomAccessibleInterval< ? extends RealType > pairwisePotentialImage ) {
		parametric.AddNode( ( int ) Views.iterable( rai ).size() ); // add as many nodes as the input image has pixels

		//her now a trick to make <3d images also comply to the code below
		IntervalView< T > ivImg = Views.interval( rai, rai );
		IntervalView< ? extends RealType > ivUnaryPotentialImage = Views.interval( unaryPotentialImage, unaryPotentialImage );
		IntervalView< ? extends RealType > ivPairwisePotentialImage = Views.interval( pairwisePotentialImage, pairwisePotentialImage );

		//make everything appear with 3 dimensions even if it is initially not
		for ( int i = 0; i < 3 - rai.numDimensions(); i++ ) {
			ivImg = Views.addDimension( ivImg, 0, 0 );
		}
		for ( int i = 0; i < 3 - ivUnaryPotentialImage.numDimensions(); i++ ) {
			ivUnaryPotentialImage = Views.addDimension( ivUnaryPotentialImage, 0, 0 );
		}
		for ( int i = 0; i < 3 - ivPairwisePotentialImage.numDimensions(); i++ ) {
			ivPairwisePotentialImage = Views.addDimension( ivPairwisePotentialImage, 0, 0 );
		}

		final long[] dims = new long[ 3 ];
		ivImg.dimensions( dims );

		final RandomAccess< T > raImg = ivImg.randomAccess();
		final RandomAccess< ? extends RealType > raUnaryPotentialImage = ivUnaryPotentialImage.randomAccess();
		final RandomAccess< ? extends RealType > raPairwisePotentialImage = ivPairwisePotentialImage.randomAccess();

		int pixelId = 0;
		final float eps = 0.0000001f;

		// for each pixel in input image --> create unary term
		for ( long z = 0; z < dims[ 2 ]; z++ ) {
			for ( long y = 0; y < dims[ 1 ]; y++ ) {
				for ( long x = 0; x < dims[ 0 ]; x++ ) {
					raUnaryPotentialImage.setPosition( new long[] { x, y, z } );

					final double likelihood = unaryCostFactor * raUnaryPotentialImage.get().getRealDouble();

					pixelId = ( int ) ( z * dims[ 1 ] * dims[ 0 ] + y * dims[ 0 ] + x );
					parametric.AddUnaryTerm( pixelId, 1.0, likelihood );
				}
			}
		}

		// for each pixel in input image --> create pairwise terms towards right (x), down (y) and back (z)
		raImg.setPosition( new long[] { 0, 0, 0 } );	// do I need this?
		raPairwisePotentialImage.setPosition( new long[] { 0, 0, 0 } );	// do I need this?
		for ( long z = 0; z < dims[ 2 ]; z++ ) {
			for ( long y = 0; y < dims[ 1 ]; y++ ) {
				for ( long x = 0; x < dims[ 0 ]; x++ ) {
					raPairwisePotentialImage.setPosition( new long[] { x, y, z } );

					final double cost_center = raPairwisePotentialImage.get().getRealDouble();
					pixelId = ( int ) ( z * dims[ 0 ] * dims[ 1 ] + y * dims[ 0 ] + x );

					if ( x + 1 < dims[ 0 ] ) {
						raPairwisePotentialImage.move( 1, 0 );
						double cost = cost_center + raPairwisePotentialImage.get().getRealDouble();
						cost /= 2;
						raPairwisePotentialImage.move( -1, 0 );

						cost += isingCost;

						final long xNeighborId = pixelId + 1;
						parametric.AddPairwiseTerm( pixelId, xNeighborId, 0.0, cost, cost, 0.0 ); // add term with costs E00, E01, E10, and E11,
					}
					if ( y + 1 < dims[ 1 ] ) {
						raPairwisePotentialImage.move( 1, 1 );
						double cost = cost_center + raPairwisePotentialImage.get().getRealDouble();
						cost /= 2;
						raPairwisePotentialImage.move( -1, 1 );

						cost += isingCost;

						final long yNeighborId = pixelId + dims[ 0 ];
						parametric.AddPairwiseTerm( pixelId, yNeighborId, 0.0, cost, cost, 0.0 ); // add term with costs E00, E01, E10, and E11,
					}
					// connect in z-direction ONLY if is3d==TRUE!!!
					if ( is3d && z + 1 < dims[ 2 ] ) {
						raPairwisePotentialImage.move( 1, 2 );
						double cost = cost_center + raPairwisePotentialImage.get().getRealDouble();
						cost /= 2;
						raPairwisePotentialImage.move( -1, 2 );

						cost += isingCost;

						final long zNeighborId = pixelId + dims[ 0 ] * dims[ 1 ];
						parametric.AddPairwiseTerm( pixelId, zNeighborId, 0.0, cost, cost, 0.0 ); // add term with costs E00, E01, E10, and E11,
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param fktUnary
	 * @param fktPairwiseX
	 * @param fktPairwiseY
	 * @param fktPairwiseZ
	 * @param potentialModulationImage
	 */
	private void buildGraph( final Function1D< Double > fktUnary, final double costIsing, final Function1D< Double > fktPairwiseX, final Function1D< Double > fktPairwiseY, final Function1D< Double > fktPairwiseZ, final RandomAccessibleInterval< ? extends RealType > potentialModulationImage ) {

		parametric.AddNode( ( int ) Views.iterable( rai ).size() ); // add as many nodes as the input image has pixels

		//her now a trick to make <3d images also comply to the code below
		IntervalView< T > ivImg = Views.interval( rai, rai );
		IntervalView< ? extends RealType > ivProbMap = ( potentialModulationImage == null ) ? null : Views.interval( potentialModulationImage, potentialModulationImage );

		//make everything appear with 3 dimensions even if it is initially not
		for ( int i = 0; i < 3 - rai.numDimensions(); i++ ) {
			ivImg = Views.addDimension( ivImg, 0, 0 );
		}
		for ( int i = 0; potentialModulationImage != null && i < 3 - potentialModulationImage.numDimensions(); i++ ) {
			ivProbMap = Views.addDimension( ivProbMap, 0, 0 );
		}

		final long[] dims = new long[ 3 ];
		ivImg.dimensions( dims );

		final RandomAccess< T > raImg = ivImg.randomAccess();
		final RandomAccess< ? extends RealType > raProbMap = ( ivProbMap == null ) ? null : ivProbMap.randomAccess();

		int pixelId = 0;
		final float eps = 0.0000001f;

		// for each pixel in input image --> create unary term
		for ( long z = 0; z < dims[ 2 ]; z++ ) {
			for ( long y = 0; y < dims[ 1 ]; y++ ) {
				for ( long x = 0; x < dims[ 0 ]; x++ ) {
					raImg.setPosition( new long[] { x, y, z } );
					if ( raProbMap != null ) {
						raProbMap.setPosition( new long[] { x, y, z } );
					}

					final double intensity = raImg.get().getRealDouble();
					double likelihood = fktUnary.evaluate( intensity ).doubleValue();
					if ( raProbMap != null ) {
						likelihood *= ( 1.0 - raProbMap.get().getRealDouble() );
					}

					pixelId = ( int ) ( z * dims[ 1 ] * dims[ 0 ] + y * dims[ 0 ] + x );
					parametric.AddUnaryTerm( pixelId, 1.0, likelihood );
				}
			}
		}

		// for each pixel in input image --> create pairwise terms towards right (x), down (y) and back (z)
		raImg.setPosition( new long[] { 0, 0, 0 } );	// do I need this?
		if ( raProbMap != null ) {
			raProbMap.setPosition( new long[] { 0, 0, 0 } );// do I need this?
		}
		for ( long z = 0; z < dims[ 2 ]; z++ ) {
			for ( long y = 0; y < dims[ 1 ]; y++ ) {
				for ( long x = 0; x < dims[ 0 ]; x++ ) {
					raImg.setPosition( new long[] { x, y, z } );
					if ( raProbMap != null ) {
						raProbMap.setPosition( new long[] { x, y, z } );
					}

					final double intensity = raImg.get().getRealDouble();
					pixelId = ( int ) ( z * dims[ 0 ] * dims[ 1 ] + y * dims[ 0 ] + x );

					if ( x + 1 < dims[ 0 ] ) {
						raImg.move( 1, 0 );
						final double intensity_next = raImg.get().getRealDouble();
						raImg.move( -1, 0 );

						double diff = Math.abs( intensity - intensity_next );
						if ( diff < eps ) diff = eps;

						double cost = costIsing + fktPairwiseX.evaluate( diff ).doubleValue(); // k2*Ising + k3*Edge
						if ( raProbMap != null ) {
							cost *= ( 1.0 - raProbMap.get().getRealDouble() );
						}

						final long xNeighborId = pixelId + 1;
						parametric.AddPairwiseTerm( pixelId, xNeighborId, 0.0, cost, cost, 0.0 ); // add term with costs E00, E01, E10, and E11,
					}
					if ( y + 1 < dims[ 1 ] ) {
						raImg.move( 1, 1 );
						final double intensity_next = raImg.get().getRealDouble();
						raImg.move( -1, 1 );

						double diff = Math.abs( intensity - intensity_next );
						if ( diff < eps ) diff = eps;

						double cost = costIsing + fktPairwiseY.evaluate( diff ).doubleValue(); // k2*Ising + k3*Edge
						if ( raProbMap != null ) {
							cost *= ( 1.0 - raProbMap.get().getRealDouble() );
						}

						final long yNeighborId = pixelId + dims[ 0 ];
						parametric.AddPairwiseTerm( pixelId, yNeighborId, 0.0, cost, cost, 0.0 ); // add term with costs E00, E01, E10, and E11,
					}
					// connect in z-direction ONLY if is3d==TRUE!!!
					if ( is3d && z + 1 < dims[ 2 ] ) {
						raImg.move( 1, 2 );
						final double intensity_next = raImg.get().getRealDouble();
						raImg.move( -1, 2 );

						double diff = Math.abs( intensity - intensity_next );
						if ( diff < eps ) diff = eps;

						double cost = costIsing + fktPairwiseZ.evaluate( diff ).doubleValue(); // k2*Ising + k3*Edge
						if ( raProbMap != null ) {
							cost *= ( 1.0 - raProbMap.get().getRealDouble() );
						}

						final long zNeighborId = pixelId + dims[ 0 ] * dims[ 1 ];
						parametric.AddPairwiseTerm( pixelId, zNeighborId, 0.0, cost, cost, 0.0 ); // add term with costs E00, E01, E10, and E11,
					}
				}
			}
		}
	}

	public long solve( final double lambdaMin, final double lambdaMax ) {
		final long solutions = parametric.Solve( lambdaMin, lambdaMax );
		System.out.println( " >>>>> ParaMaxFlow solutions found: " + solutions + " <<<<<" );
		regionsImg = createRegionsImg();
		return solutions;
	}

	private Img< LongType > createRegionsImg() {
		long[] dims = new long[ rai.numDimensions() ];
		rai.dimensions( dims );
		final ImgFactory< LongType > imgFactory = new ArrayImgFactory< LongType >();
		final Img< LongType > ret = imgFactory.create( dims, new LongType() );

		//here now a trick to make <3d images also comply to the code below
		IntervalView< LongType > ivRet = Views.interval( ret, ret );
		for ( int i = 0; i < 3 - rai.numDimensions(); i++ ) {
			ivRet = Views.addDimension( ivRet, 0, 0 );
		}
		final RandomAccess< LongType > raRet = ivRet.randomAccess();

		dims = new long[ ivRet.numDimensions() ];
		ivRet.dimensions( dims );

		for ( long graphNodeId = 0; graphNodeId < Views.iterable( rai ).size(); graphNodeId++ ) {
			final long numRegions = parametric.GetRegionCount( parametric.GetRegion( graphNodeId ) );

			final long z = graphNodeId / ( dims[ 0 ] * dims[ 1 ] );
			final long remainder = graphNodeId - z * ( dims[ 0 ] * dims[ 1 ] );
			final long y = remainder / dims[ 0 ];
			final long x = remainder - y * dims[ 0 ];

			raRet.setPosition( new long[] { x, y, z } );
			raRet.get().set( numRegions );
		}

		return ret;
	}

	public Img< LongType > getRegionsImg() {
		return regionsImg;
	}

	public Img< BitType > getSolution( final long solutionId ) {
		final long[] dims = new long[ rai.numDimensions() ];
		rai.dimensions( dims );
		final ImgFactory< BitType > imgFactory = new ArrayImgFactory< BitType >();
		final Img< BitType > ret = imgFactory.create( dims, new BitType() );

		//here now a trick to make <3d images also comply to the code below
		IntervalView< BitType > ivRet = Views.interval( ret, ret );
		for ( int i = 0; i < 3 - rai.numDimensions(); i++ ) {
			ivRet = Views.addDimension( ivRet, 0, 0 );
		}
		final RandomAccess< BitType > raRet = ivRet.randomAccess();

		for ( long graphNodeId = 0; graphNodeId < Views.iterable( rai ).size(); graphNodeId++ ) {
			final long numRegions = parametric.GetRegionCount( parametric.GetRegion( graphNodeId ) );

			final long z = graphNodeId / ( dims[ 0 ] * dims[ 1 ] );
			final long remainder = graphNodeId - z * ( dims[ 0 ] * dims[ 1 ] );
			final long y = remainder / dims[ 0 ];
			final long x = remainder - y * dims[ 0 ];

			raRet.setPosition( new long[] { x, y, z } );
			raRet.get().set( ( numRegions < solutionId ) );
		}

		return ret;
	}
}
