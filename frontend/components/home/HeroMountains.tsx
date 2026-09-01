"use client";

import { useRef, useMemo, Suspense, type MutableRefObject } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import * as THREE from "three";
import { useReducedMotion } from "@/lib/hooks/useReducedMotion";

type HeroMountainsProps = {
  scrollRef?: MutableRefObject<number>;
};

function createRidgeGeometry(width: number, height: number, seed: number) {
  const geo = new THREE.PlaneGeometry(width, height, 96, 32);
  const pos = geo.attributes.position;
  for (let i = 0; i < pos.count; i++) {
    const x = pos.getX(i);
    const y = pos.getY(i);
    const t = (y + height / 2) / height;
    const ridge =
      Math.sin(x * 0.28 + seed) * 1.1 +
      Math.sin(x * 0.72 + seed * 1.7) * 0.55 +
      Math.sin(x * 1.6 + seed * 0.4) * 0.22 +
      Math.sin(x * 3.4 + seed) * 0.08;
    pos.setY(i, y + ridge * t * t * height * 0.42);
    pos.setZ(i, Math.cos(x * 0.35 + seed) * t * 0.55);
  }
  geo.computeVertexNormals();
  return geo;
}

function MountainRidge({
  color,
  z,
  y,
  width,
  height,
  seed,
  scrollRef,
  parallax,
}: {
  color: string;
  z: number;
  y: number;
  width: number;
  height: number;
  seed: number;
  scrollRef?: MutableRefObject<number>;
  parallax: number;
}) {
  const meshRef = useRef<THREE.Mesh>(null);
  const geometry = useMemo(
    () => createRidgeGeometry(width, height, seed),
    [width, height, seed]
  );

  useFrame((state) => {
    const mesh = meshRef.current;
    if (!mesh) return;
    const scroll = scrollRef?.current ?? 0;
    const drift = Math.sin(state.clock.elapsedTime * 0.12 + seed) * 0.04;
    mesh.position.y = y + drift - scroll * parallax * 0.6;
    mesh.position.z = z - scroll * parallax * 1.4;
  });

  return (
    <mesh ref={meshRef} geometry={geometry} position={[0, y, z]}>
      <meshStandardMaterial
        color={color}
        roughness={0.82}
        metalness={0.04}
        side={THREE.DoubleSide}
      />
    </mesh>
  );
}

function Water({ scrollRef }: { scrollRef?: MutableRefObject<number> }) {
  const meshRef = useRef<THREE.Mesh>(null);
  const geometry = useMemo(() => {
    const geo = new THREE.PlaneGeometry(48, 28, 56, 32);
    geo.rotateX(-Math.PI / 2);
    return geo;
  }, []);

  const basePositions = useMemo(() => {
    const pos = geometry.attributes.position;
    return Float32Array.from(pos.array as Float32Array);
  }, [geometry]);

  useFrame((state) => {
    const mesh = meshRef.current;
    if (!mesh) return;
    const t = state.clock.elapsedTime;
    const pos = geometry.attributes.position;
    for (let i = 0; i < pos.count; i++) {
      const x = basePositions[i * 3];
      const z = basePositions[i * 3 + 2];
      pos.setY(
        i,
        Math.sin(x * 0.45 + t * 1.15) * 0.07 +
          Math.sin(z * 0.55 + t * 0.85) * 0.055 +
          Math.sin((x + z) * 0.25 + t * 0.4) * 0.03
      );
    }
    pos.needsUpdate = true;

    const scroll = scrollRef?.current ?? 0;
    mesh.position.y = -1.35 - scroll * 0.25;
  });

  return (
    <mesh ref={meshRef} geometry={geometry} position={[0, -1.35, 2]}>
      <meshStandardMaterial
        color="#3a8fa8"
        roughness={0.18}
        metalness={0.35}
        transparent
        opacity={0.92}
      />
    </mesh>
  );
}

function SunGlow() {
  return (
    <mesh position={[6.5, 3.8, -10]}>
      <sphereGeometry args={[0.7, 16, 16]} />
      <meshBasicMaterial color="#f4d9a8" />
    </mesh>
  );
}

function ScrollCamera({ scrollRef }: { scrollRef?: MutableRefObject<number> }) {
  useFrame(({ camera }) => {
    const scroll = scrollRef?.current ?? 0;
    camera.position.z = THREE.MathUtils.lerp(7.2, 5.2, scroll);
    camera.position.y = THREE.MathUtils.lerp(1.15, 1.7, scroll);
    camera.rotation.x = THREE.MathUtils.lerp(-0.12, -0.18, scroll);
  });
  return null;
}

function Scene({ scrollRef }: { scrollRef?: MutableRefObject<number> }) {
  return (
    <>
      <ScrollCamera scrollRef={scrollRef} />
      <fog attach="fog" args={["#c9e4ef", 8, 28]} />
      <ambientLight intensity={0.55} color="#e8f4f8" />
      <directionalLight position={[8, 10, 4]} intensity={1.35} color="#fff6e8" />
      <directionalLight position={[-6, 3, 2]} intensity={0.45} color="#7ec8d4" />
      <hemisphereLight args={["#b8dff0", "#2d6a4f", 0.55]} />

      <SunGlow />

      <MountainRidge
        color="#163a4a"
        z={-14}
        y={0.2}
        width={36}
        height={10}
        seed={0.4}
        scrollRef={scrollRef}
        parallax={0.25}
      />
      <MountainRidge
        color="#1b4d3e"
        z={-9}
        y={-0.15}
        width={30}
        height={8.5}
        seed={1.6}
        scrollRef={scrollRef}
        parallax={0.45}
      />
      <MountainRidge
        color="#246b4e"
        z={-5.2}
        y={-0.45}
        width={26}
        height={6.8}
        seed={2.8}
        scrollRef={scrollRef}
        parallax={0.7}
      />
      <MountainRidge
        color="#2f855a"
        z={-2.4}
        y={-0.75}
        width={22}
        height={5.2}
        seed={0.9}
        scrollRef={scrollRef}
        parallax={1}
      />
      <Water scrollRef={scrollRef} />
    </>
  );
}

function StaticFallback() {
  return (
    <div className="hero-landscape-static h-full w-full" />
  );
}

export function HeroMountains({ scrollRef }: HeroMountainsProps) {
  const reducedMotion = useReducedMotion();

  if (reducedMotion) {
    return <StaticFallback />;
  }

  return (
    <Canvas
      camera={{ position: [0, 1.15, 7.2], fov: 46 }}
      className="h-full w-full"
      dpr={[1, 1.6]}
      gl={{ antialias: true, alpha: true }}
    >
      <Suspense fallback={null}>
        <Scene scrollRef={scrollRef} />
      </Suspense>
    </Canvas>
  );
}
