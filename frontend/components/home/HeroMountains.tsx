"use client";

import { useRef, useMemo, Suspense, type MutableRefObject } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import * as THREE from "three";
import { useReducedMotion } from "@/lib/hooks/useReducedMotion";

type HeroMountainsProps = {
  /** Updated on scroll (0 = top of hero, 1 = hero scrolled away) */
  scrollRef?: MutableRefObject<number>;
};

function MountainLayer({
  baseColor,
  y,
  scale,
  speed,
  scrollRef,
  depthOffset,
}: {
  baseColor: THREE.Color;
  y: number;
  scale: number;
  speed: number;
  scrollRef?: MutableRefObject<number>;
  depthOffset: number;
}) {
  const meshRef = useRef<THREE.Mesh>(null);
  const materialRef = useRef<THREE.MeshStandardMaterial>(null);
  const accentColor = useMemo(() => new THREE.Color("#d4a574"), []);
  const dayColor = useMemo(() => new THREE.Color("#52b788"), []);

  const geometry = useMemo(() => {
    const geo = new THREE.PlaneGeometry(14, 4.5, 48, 20);
    const pos = geo.attributes.position;
    for (let i = 0; i < pos.count; i++) {
      const x = pos.getX(i);
      const wave =
        Math.sin(x * 1.2) * 0.35 +
        Math.sin(x * 2.4 + 1) * 0.18 +
        Math.sin(x * 4.1) * 0.08;
      pos.setZ(i, wave);
    }
    geo.computeVertexNormals();
    return geo;
  }, []);

  useFrame((state) => {
    const scroll = scrollRef?.current ?? 0;
    const mesh = meshRef.current;
    const material = materialRef.current;
    if (!mesh || !material) return;

    const bob = Math.sin(state.clock.elapsedTime * speed) * 0.04;
    mesh.position.y = y - scroll * 0.35 + bob;
    mesh.position.z = -scale - depthOffset - scroll * 1.2;
    mesh.rotation.x = -0.08 - scroll * 0.05;

    const blended = baseColor.clone();
    if (scroll < 0.5) {
      blended.lerp(dayColor, scroll * 1.4);
    } else {
      blended.lerp(accentColor, (scroll - 0.5) * 1.6);
    }
    material.color.copy(blended);
    material.opacity = 0.55 + scroll * 0.15;
  });

  return (
    <mesh
      ref={meshRef}
      geometry={geometry}
      position={[0, y, -scale - depthOffset]}
      scale={[scale, scale * 0.42, 1]}
    >
      <meshStandardMaterial
        ref={materialRef}
        color={baseColor}
        transparent
        opacity={0.6}
        side={THREE.DoubleSide}
      />
    </mesh>
  );
}

function ScrollCamera({ scrollRef }: { scrollRef?: MutableRefObject<number> }) {
  useFrame(({ camera }) => {
    const scroll = scrollRef?.current ?? 0;
    camera.position.z = THREE.MathUtils.lerp(5.2, 3.4, scroll);
    camera.position.y = THREE.MathUtils.lerp(0, 0.45, scroll);
    camera.rotation.x = THREE.MathUtils.lerp(0, -0.08, scroll);
  });
  return null;
}

function Scene({ scrollRef }: { scrollRef?: MutableRefObject<number> }) {
  const colors = useMemo(
    () => ({
      back: new THREE.Color("#1b4332"),
      mid: new THREE.Color("#2d6a4f"),
      front: new THREE.Color("#40916c"),
    }),
    []
  );

  return (
    <>
      <ScrollCamera scrollRef={scrollRef} />
      <ambientLight intensity={0.85} />
      <directionalLight position={[4, 6, 5]} intensity={0.55} color="#faf9f7" />
      <directionalLight
        position={[-3, 2, 2]}
        intensity={0.25}
        color="#d4a574"
      />
      <MountainLayer
        baseColor={colors.back}
        y={-0.45}
        scale={2.1}
        speed={0.28}
        scrollRef={scrollRef}
        depthOffset={0}
      />
      <MountainLayer
        baseColor={colors.mid}
        y={-0.75}
        scale={2.6}
        speed={0.2}
        scrollRef={scrollRef}
        depthOffset={0.4}
      />
      <MountainLayer
        baseColor={colors.front}
        y={-1.05}
        scale={3.2}
        speed={0.14}
        scrollRef={scrollRef}
        depthOffset={0.8}
      />
    </>
  );
}

function StaticFallback() {
  return (
    <div className="h-full w-full bg-gradient-to-b from-forest/15 via-forest/5 to-transparent" />
  );
}

export function HeroMountains({ scrollRef }: HeroMountainsProps) {
  const reducedMotion = useReducedMotion();

  if (reducedMotion) {
    return <StaticFallback />;
  }

  return (
    <Canvas
      camera={{ position: [0, 0, 5.2], fov: 48 }}
      className="h-full w-full"
      dpr={[1, 1.5]}
      gl={{ antialias: false, alpha: true }}
    >
      <Suspense fallback={null}>
        <Scene scrollRef={scrollRef} />
      </Suspense>
    </Canvas>
  );
}
